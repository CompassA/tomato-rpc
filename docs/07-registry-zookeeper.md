# 注册中心（ZooKeeper）

## 定位

注册中心负责两件事：**服务端把自己挂上去**、**客户端把别人的实例列表拉下来并持续跟随变化**。抽象接口在 core（`NameServer`），实现只有一份：`tomato-rpc-registry-zookeeper`。

```text
NameServer（core 接口）
   └── BaseNameServer（core：refresh 队列 + invoker-refresh-thread）
         └── ZookeeperNameServer（ZK 实现，命名空间 tomato）
               └── ZookeeperRegistry（真正的读写逻辑）
                     ├── CuratorClient（Curator 封装）
                     ├── PathChildrenListener（子节点 Watcher）
                     └── ZookeeperAssembler（路径与 URL 编解码）
```

## ZooKeeper 上的数据结构

Curator 客户端设置了 `namespace("tomato")`，所以下面的路径都是**逻辑路径**，实际 ZK 上是 `/tomato/...`。

```text
/tomato
└── {microServiceId}                       ← URL 编码后的微服务 id
    ├── {stage}                            ← URL 编码后的环境
    │   ├── providers
    │   │   ├── {URLEncoder(实例URI)}       ← 临时节点(EPHEMERAL)，值无意义
    │   │   └── ...
    │   └── routers
    │       └── {目标microServiceId}        ← 持久节点，值为 JSON(RpcRouterData)
    └── {另一个stage}
        └── providers/...
```

| 节点                 | 类型         | 内容                                                                        | 谁写                                            |
| -------------------- | ------------ | --------------------------------------------------------------------------- | ----------------------------------------------- |
| `providers/{url}`    | `EPHEMERAL`  | 空                                                                          | 服务端 `registerService`                        |
| `routers/{targetId}` | `PERSISTENT` | `RpcRouterData` 的 JSON，**只存表达式列表**（`{"expr":[...]}`），不含版本号 | dashboard（见 [监控与运营](./11-dashboard.md)） |

> 注意 `convertToZNodePath` 对**每一段**都做了 `URLEncoder.encode`，包括 `microServiceId` 和 `stage`。所以路径里看到 `%3A`、`%2F` 是正常的，读路径时必须先 `URLDecoder.decode`。

### 实例 URL 格式

`MetaData.URL_FORMAT` 决定了 provider 节点名：

```text
{protocol}://{host}:{port}/?micro-service-id={id}&stage={stage}&group={group}&property=weight:1
```

示例：

```text
tomato://192.168.1.5:4567/?micro-service-id=demo-rpc-service&stage=dev&group=main&property=weight:1
```

| 参数                                   | 来源                                | 用途                        |
| -------------------------------------- | ----------------------------------- | --------------------------- |
| `protocol`                             | `RpcConfig.protocol`（`tomato`）    | 校验 + 展示                 |
| `host`                                 | `NetworkUtil.getLocalHost()`        | 连接目标                    |
| `port`                                 | `RpcConfig.port`                    | 连接目标                    |
| `micro-service-id` / `stage` / `group` | 元数据                              | 路由兜底规则按 `group` 匹配 |
| `property`                             | `NodeProperty.toUrl()` → `weight:1` | 负载均衡权重                |

反序列化方向：`ZookeeperAssembler.convertToModel` 先 `URLDecoder.decode` 再 `MetaData.convert(URI)`；`MetaData#isValid` 会校验 protocol/host/microServiceId/stage/group 均非空，不合法直接丢弃并打 warn。

## 注册

```java
// ZookeeperRegistry#register
Optional<URI> uriOpt = MetaData.convert(metaData);
String zNodePath = ZookeeperAssembler.buildServiceNodePath(
    metaData.getMicroServiceId(), metaData.getStage(), uriOpt.get());
curatorClient.createEphemeral(zNodePath);
```

- 用**临时节点**：进程崩溃、会话超时（60s）后 ZK 自动摘除，不需要兜底的健康检查。
- 重名节点会抛 `NodeExistsException`，由 `createEphemeral` 内部处理（已存在即视为成功）。
- 属性变更（`updateServerProperty`）会以**新 URL** 重新注册，因此节点名会变，旧节点要靠 `unregister` 或会话结束才消失。

## 订阅

订阅分两段：先拉实例，再读路由规则。

```java
for (MicroServiceSpace microService : microServices) {
    String targetPath = ZookeeperAssembler.buildServiceNodeParent(microServiceId, stage);
    ChildrenListener listener = listenerMap.computeIfAbsent(microService,
        service -> new PathChildrenListener(microService, owner, curatorClient));
    List<String> invokerUrlList = curatorClient.getChildrenAndAddWatcher(targetPath, listener);
    ...
    microService.refresh(metadata);      // 首次全量
}

for (MicroServiceSpace microService : microServices) {
    String path = ZookeeperAssembler.buildServiceRouterPath(
        rpcServerMetaData.getMicroServiceId(), rpcServerMetaData.getStage(), microServiceId);
    if (curatorClient.checkExists(path) != null) {
        RpcRouterData routerData = ZookeeperAssembler.toRouterData(curatorClient.getData(path));
        microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, routerData.getExpr());
    } else {
        microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, Collections.emptyList());
    }
}
```

两个容易看错的地方：

1. **路由规则路径用的是「消费者」的 id**：`/tomato/{consumerId}/{stage}/routers/{providerId}`。灰度规则是「谁消费、怎么路由」，不是「谁提供」。
2. `refreshRouter(0, emptyList)` 并不是「清空路由」，而是「只装兜底规则」。`refreshRouter` 内部会 `newRouters.addAll(defaultRouters)`，兜底规则永远在最后。

`getChildrenAndAddWatcher` 在路径不存在时会先 `creatingParentsIfNeeded` 建一个持久父节点，避免首次订阅时因父节点缺失而报错。

## 变更通知

ZooKeeper 的 watcher 是**一次性的**，所以回调里必须重新注册：

```java
@Override
public void process(WatchedEvent watchedEvent) {
    if (unwatch) return;
    String path = watchedEvent.getPath();
    List<String> children = curatorClient.getChildrenAndAddWatcher(path, this);  // 重新注册
    childrenChanged(path, children);
}
```

`childrenChanged` 把子节点 URL 转成 `Set<MetaData>`，然后**不直接 refresh**，而是投递给 NameServer：

```java
nameServer.submitInvokerRefreshTask(new RefreshInvokerTask(uniqueId, microService, invokerInfo));
```

`BaseNameServer` 用一个容量 1000 的 `ArrayBlockingQueue` + 单线程 `invoker-refresh-thread` 串行消费：

| 设计点             | 原因                                                                       |
| ------------------ | -------------------------------------------------------------------------- |
| 单线程串行         | `refresh` 要建/关连接，并发执行容易出现「刚建的 invoker 被另一个线程关掉」 |
| 队列而非直接调用   | watcher 回调运行在 Curator 的事件线程上，重活（建连接）不能占着它          |
| `put` 而非 `offer` | 队列满时阻塞 watcher 线程，宁可慢也不丢事件（代价：ZK 事件线程可能被打满） |

每次 refresh 都会打摘要日志：

```text
|zookeeper|watcher-receive|Y|{path}|{耗时}|
|name-server|invoker-refresh|req|{任务id}|
|name-server|invoker-refresh|res|{Y/N}|{耗时}|{异常类名}|{任务id}|
```

## 实例列表如何变成 invoker

`BaseMicroServiceSpace#refresh` 的规则很简单：**能复用就复用，不能复用就新建，消失的就关掉**。

| 情况                            | 处理                                                                                                     |
| ------------------------------- | -------------------------------------------------------------------------------------------------------- |
| 节点集合为空                    | `cleanInvoker()`：清空列表并关闭所有 invoker                                                             |
| 节点已存在（`MetaData.equals`） | 复用原 invoker，连接不重建                                                                               |
| 新节点                          | `doCreateInvoker` → `NettyRpcInvokerFactory#create` → 新 `NettyRpcInvoker`（此时还不连接，首次调用才连） |
| 旧节点消失                      | `doCloseInvoker` → `destroy()`（含最多 60s 排空）                                                        |
| `enableCircuit = true`          | 新 invoker 外面再包一层 `CircuitRpcInvoker`                                                              |

`MetaData#equals` **只比较** `protocol/host/port/microServiceId/stage/group`，不比较 `nodeProperty`。所以「只改权重」的场景下，节点 URL 变了、ZK 子节点变了，但消费者会认为「还是那个实例」而复用旧 invoker，`getInvokerPropertyMap()` 里仍是旧权重——这是当前实现的一个已知偏差。

## 反注册与取消订阅

```java
// BaseRpcCoreService#doStop
nameServer.unsubscribe(microServices, stage);
nameServer.unregisterService(rpcServerMetaData);
for (MicroServiceSpace microService : microServices) microService.close();
rpcServer.stop();
nameServer.stop();
```

| 动作                | 行为                                                                                                           |
| ------------------- | -------------------------------------------------------------------------------------------------------------- |
| `unregisterService` | 删除 provider 临时节点（`delete().guaranteed().deletingChildrenIfNeeded()`）                                   |
| `unsubscribe`       | 从 `microServiceMap` 移除、`close()` 掉 `MicroServiceSpace`（关闭所有连接）、`unwatch()` 并把监听器从 map 移除 |
| `stop`              | `refreshInvokerThread.interrupt()` + 关闭 Curator 客户端                                                       |

## 容错参数

| 项        | 值                                 | 位置                                |
| --------- | ---------------------------------- | ----------------------------------- |
| namespace | `tomato`                           | `ZookeeperNameServer.ZK_NAME_SPACE` |
| 连接超时  | 15s                                | `CuratorClient`                     |
| 会话超时  | 60s                                | 决定临时节点多久后消失              |
| 重试      | `ExponentialBackoffRetry(1000, 3)` | 首次 1s，最多 3 次                  |
| 刷新队列  | `ArrayBlockingQueue(1000)`         | `BaseNameServer`                    |

## 已知限制

| #   | 限制                                         | 影响                                                                |
| --- | -------------------------------------------- | ------------------------------------------------------------------- |
| 1   | 只监听子节点变化，不监听 `NODE_DATA_CHANGED` | `routers` 节点的数据更新不会实时生效，需要客户端重启或重新订阅      |
| 2   | 路由刷新靠 dashboard 主动 HTTP 推送          | ZK 只作为规则的持久化存储，不承担推送                               |
| 3   | `MetaData.equals` 忽略 `nodeProperty`        | 只改权重不生效（见上）                                              |
| 4   | 刷新队列满时阻塞 watcher                     | ZK 事件线程可能被拖慢                                               |
| 5   | 无本地缓存持久化                             | 启动时 ZK 不可用则服务不可用                                        |
| 6   | `PathChildrenListener` 未使用 `TreeCache`    | `CuratorClient` 里有 `subscribe`/`TreeCache` 封装，但注册中心没走它 |

## 关键类速查

| 类                     | 位置                                                                                                                                             | 职责                      |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------- |
| `NameServer`           | [core/registry](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/registry/NameServer.java)                                             | 注册/发现/通知抽象        |
| `BaseNameServer`       | [core/registry](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/registry/BaseNameServer.java)                                         | 刷新队列 + 单线程消费     |
| `ZookeeperNameServer`  | [registry-zookeeper](../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/ZookeeperNameServer.java)            | ZK 实现入口、namespace    |
| `ZookeeperRegistry`    | [registry-zookeeper/impl](../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/ZookeeperRegistry.java)    | 注册/订阅/查询            |
| `PathChildrenListener` | [registry-zookeeper/impl](../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/PathChildrenListener.java) | 子节点 Watcher            |
| `ZookeeperAssembler`   | [registry-zookeeper/utils](../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/utils/ZookeeperAssembler.java) | 路径拼接、URL/JSON 编解码 |
| `CuratorClient`        | [registry-zookeeper](../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/CuratorClient.java)                  | Curator 封装              |
| `MetaData`             | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/MetaData.java)                                                       | 实例元数据与 URL 格式     |
