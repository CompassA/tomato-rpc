# tomato-rpc-registry-zookeeper

> 注册中心的 ZooKeeper 实现：把「有哪些实例」「有哪些路由规则」写进 ZK，并在实例增减时刷新本地内存。它是 `tomato-rpc-netty` 的默认搭档（netty 模块主动依赖它），所以业务方通常不用单独引。

## 职责

| 它做什么                                                           | 它不做什么                                           |
| ------------------------------------------------------------------ | ---------------------------------------------------- |
| 把本机 `MetaData` 写成 ZK 临时节点（注册）                         | 决定注册谁（`BaseRpcCoreService#doStart` 决定）      |
| 订阅下游服务的实例列表，实例变更时回调                             | 决定刷新后选哪个实例（`BaseMicroServiceSpace` 决定） |
| 启动时读取下游的路由表达式交给 `refreshRouter`                     | 解析表达式（`tomato-rpc-expression` 负责）           |
| 提供 `getMicroService` / `listInvokers` / `lookupInvoker` 查询入口 | 提供 HTTP 接口（config 的 `MonitorController` 负责） |

## 依赖

```text
tomato-rpc-registry-zookeeper
├── tomato-rpc-core          // NameServer / MetaData / MicroServiceSpace 等抽象
├── tomato-rpc-common        // Logger
├── curator-framework        // 5.9.0
├── curator-recipes          // 只为锁和测试；生产代码里没用到 TreeCache
└── zookeeper                // 5.5.9（对齐 curator）
```

注意方向：**本模块不依赖 netty**，netty 反过来依赖它。所以想换注册中心时，只要提供一个新的 `NameServerFactory` 实现。

## 类清单

| 类                                                                                                                                                                               | 职责                                                                                                                              |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| [ZookeeperNameServer](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/ZookeeperNameServer.java)                                        | `BaseNameServer` 的子类，纯转发；`doInit` 里固定 `namespace = "tomato"`                                                           |
| [ZookeeperRegistry](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/ZookeeperRegistry.java)                                       | 真正的逻辑：`register`/`unregister`/`subscribe`/`unsubscribe`/`lookup`/`listInvokers`                                             |
| [CuratorClient](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/CuratorClient.java)                                                    | 对 `CuratorFramework` 的薄封装，所有路径都在 namespace 之下                                                                       |
| [PathChildrenListener](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/PathChildrenListener.java)                                 | 一次性 watcher：收到事件 → 重新 `getChildren` 并重新挂 watcher → 提交刷新任务                                                     |
| `ChildrenListener` / `WatcherHandler`                                                                                                                                            | 监听器接口；`WatcherHandler` 是基于 `TreeCacheListener` 的抽象基类，**生产代码未使用**（只有 `CuratorClientTest` 用到 TreeCache） |
| [ZookeeperAssembler](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/utils/ZookeeperAssembler.java)                                    | 路径拼装、URL 编解码、路由规则 JSON 序列化                                                                                        |
| `ZookeeperConfig` / [ZookeeperNameServerFactory](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/ZookeeperNameServerFactory.java) | 配置对象 / SPI 工厂（key `zookeeper-factory`）                                                                                    |

## ZK 树结构

namespace 固定为 `tomato`，所以下面所有路径在 ZK 客户端里看都带 `/tomato` 前缀：

```text
/tomato
└── {microServiceId}                    ← 服务 id 本身被 URLEncoder 编码
    └── {stage}
        ├── providers
        │   └── {enc(protocol://host:port/?micro-service-id=..&stage=..&group=..&property=weight:1)}
        └── routers
            └── {routerMicroServiceId}   ← 值为 {"expr":["...","..."]}，PERSISTENT
```

| 节点          | 类型       | 数据                                                    |
| ------------- | ---------- | ------------------------------------------------------- |
| `providers/*` | EPHEMERAL  | **空**，所有信息都在节点名里                            |
| `routers/*`   | PERSISTENT | `ZookeeperAssembler.toRouterBytes(RpcRouterData)`，JSON |

实例节点名的原始形态（编码前）是 `MetaData.URL_FORMAT`：

```text
tomato://127.0.0.1:4567/?micro-service-id=demo-rpc-service&stage=dev&group=main&property=weight:1
```

`ZookeeperAssembler.convertToZNodePath` 对**每一段**都做 `URLEncoder.encode`，所以 `:`、`/`、`?`、`&`、`=` 会变成 `%3A`、`%2F`、`%3F`…；读回来时 `convertToModel` 先 `URLDecoder.decode` 再 `URI.create`。

顺带一个限制：`MetaData.NodeProperty.toUrl()` 目前只输出 `weight:{n}`，其它键（`#` 分隔）能解析但不会写入——想说「实例上加自定义标签」目前做不到。

## 注册 / 注销

```java
public void register(MetaData metaData) throws Exception {
    Optional<URI> uriOpt = MetaData.convert(metaData);
    if (uriOpt.isEmpty()) { return; }          // 元数据不合法就静默跳过
    String zNodePath = ZookeeperAssembler.buildServiceNodePath(
        metaData.getMicroServiceId(), metaData.getStage(), uriOpt.get());
    curatorClient.createEphemeral(zNodePath);
}
```

`create` 带 `creatingParentsIfNeeded()`，所以 `/tomato`、服务 id、stage、`providers` 这些父节点不用预建。注销就是 `delete(guaranteed + deletingChildrenIfNeeded)`。

EPHEMERAL 的语义：**会话断开节点自动消失**。这既是优点（进程崩了实例自动下线），也是坑——网络抖动导致 session 过期时，实例会从 ZK 消失，而本进程没有重连后重新注册的逻辑（没有 `ConnectionStateListener`）。

## 订阅

`subscribe` 做两件事，顺序不能反：

```java
// 1. 实例列表：先拉一次全量，再挂 watcher
String targetPath = ZookeeperAssembler.buildServiceNodeParent(microServiceId, stage);
ChildrenListener listener = listenerMap.computeIfAbsent(microService,
    service -> new PathChildrenListener(microService, owner, curatorClient));
List<String> invokerUrlList = curatorClient.getChildrenAndAddWatcher(targetPath, listener);
microService.refresh(metadata);

// 2. 路由规则：只读一次，没有 watcher
String path = ZookeeperAssembler.buildServiceRouterPath(
    rpcServerMetaData.getMicroServiceId(), rpcServerMetaData.getStage(), microServiceId);
if (curatorClient.checkExists(path) != null) {
    microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, routerData.getExpr());
} else {
    microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, Collections.emptyList());
}
```

几个容易忽略的点：

- `getChildrenAndAddWatcher` 里有个「路径不存在就先建成 PERSISTENT」的兜底，所以订阅一个还没人注册的服务不会报错，只是拿到空列表。
- 路由规则**没有 watcher**，只在下发时由 dashboard 主动 HTTP 推送给每个实例（见 [监控与运营](./../11-dashboard.md)）。实例重启后靠这里的「读一次」恢复。
- 启动时用 `INITIAL_ROUTER_OPS_ID`（=0）提交，而 `BaseMicroServiceSpace` 内存里的 `opsGlobalId` 初值是 `-1`，所以第一次一定生效。
- 订阅的实例和路由都要经过 `microService.refresh(...)`，那是 `BaseMicroServiceSpace` 的活，不在本模块。

## 变更如何进内存

watcher 在 ZK 的事件线程上跑，刷新在另一个线程上串行做，中间隔一个队列：

```text
ZK 事件线程
  PathChildrenListener#process
    → getChildrenAndAddWatcher（重新挂一次性 watcher）
    → nameServer.submitInvokerRefreshTask(task)     // ArrayBlockingQueue(1000)，put 阻塞
          ▼
invoker-refresh-thread（BaseNameServer#doInit 里创建的单个守护线程）
  task.getMicroServiceSpace().refresh(task.getInvokerInfoSet())
```

| 细节                      | 说明                                                                                              |
| ------------------------- | ------------------------------------------------------------------------------------------------- |
| 队列容量 1000、`put` 阻塞 | 消费者只有一个线程，实例数量巨大且频繁抖动时 ZK 事件线程会被反压                                  |
| 单线程串行                | 天然避免「旧快照后到覆盖新快照」；`RefreshInvokerTask` 自带的 `uniqueId` 只用于日志，没做乱序丢弃 |
| 异常只打日志              | `refresh` 抛异常时该次刷新丢失，等下一次事件                                                      |
| 摘要日志                  | `\|zookeeper\|watcher-receive\|Y/N\|path\|耗时\|` 与 `\|name-server\|invoker-refresh\|...`        |

`ZookeeperRegistry.microServiceMap` 是普通 `HashMap`，只在启动/关闭阶段写；`listenerMap` 是 `ConcurrentHashMap`。`unsubscribe` 里会 `provider.close()` 并 `listener.unwatch()`（只是把 `unwatch` 标志置真，watcher 仍会在下一次事件时返回）。

## Curator 连接参数

```java
CuratorFrameworkFactory.builder()
    .connectString(zookeeperConfig.getConnString())
    .retryPolicy(new ExponentialBackoffRetry(1000, 3))   // 最多重试 3 次
    .connectionTimeoutMs(15 * 1000)
    .sessionTimeoutMs(60 * 1000)
    .namespace("tomato")
    .build();
```

对比 dashboard 自己起的那个 Curator：`ExponentialBackoffRetry(1000, 1000000)`（几乎无限重试）——同一个 ZK，两套重试策略。

## 扩展点

| 想做的事       | 做法                                                                                                                       |
| -------------- | -------------------------------------------------------------------------------------------------------------------------- |
| 换注册中心     | 实现 core 的 `NameServer` + `NameServerFactory`，写 `META-INF/tomato/org.tomato.study.rpc.core.registry.NameServerFactory` |
| 用别的 ZK 实现 | 当前文件内容是 `zookeeper-factory : ...ZookeeperNameServerFactory`，改 value 即可                                          |
| 换 namespace   | `ZookeeperNameServer` 里是硬编码常量 `ZK_NAME_SPACE = "tomato"`，需要改代码                                                |

## 已知问题

| #   | 问题                                                             | 影响                                                                  |
| --- | ---------------------------------------------------------------- | --------------------------------------------------------------------- |
| 1   | 没有 `ConnectionStateListener`，session 过期后不重新注册         | 实例从 ZK 消失但进程还活着，调用方永远发现不了它                      |
| 2   | 路由规则没有 watcher                                             | 只能靠 dashboard 推送；推送失败（HTTP 9090 不通）时实例用旧规则       |
| 3   | 一次性 watcher 有丢事件窗口                                      | `process` 里「重新拉 + 重新挂」之间发生的变更可能漏掉，要等下一次变更 |
| 4   | `convertToModel` 遇到坏节点名直接抛异常                          | `childrenChanged` 整体失败，一批实例都不会刷新                        |
| 5   | `register` 时元数据不合法静默 return                             | 日志里没有痕迹，表现为「服务起来了但注册不上」                        |
| 6   | `WatcherHandler` + `CuratorClient#subscribe(TreeCache)` 是死代码 | 读代码时容易误以为「路由/实例走 TreeCache」                           |
| 7   | 路由规则存储在 ZK 但没有版本号                                   | 并发下发靠 `/lock`（dashboard 侧），ZK 里的值本身无法判断新旧         |

## 测试

| 测试                      | 覆盖                                                                                     |
| ------------------------- | ---------------------------------------------------------------------------------------- |
| `ZookeeperNameServerTest` | 用 curator-test 的 `TestingServer(5422)` 真起一个 ZK，注册后断言节点存在、注销后断言消失 |
| `CuratorClientTest`       | 增删改查 + `subscribe`（TreeCache）回调                                                  |
| `BaseMicroServiceTest`    | 不碰 ZK，纯内存验证 `refresh` 后路由/负载均衡结果                                        |

详见 [测试](./../13-testing.md)。

## 阅读顺序

1. [ZookeeperAssembler](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/utils/ZookeeperAssembler.java)：先弄清路径怎么拼、节点名怎么编解码。
2. [ZookeeperRegistry](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/ZookeeperRegistry.java)：注册与订阅的主流程。
3. [PathChildrenListener](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/impl/PathChildrenListener.java) + `BaseNameServer`：变更怎么排队进内存。
4. [CuratorClient](../../tomato-rpc-registry-zookeeper/src/main/java/org/tomato/study/rpc/registry/zookeeper/CuratorClient.java)：连接参数与重试。

配套章节：[注册中心](./../07-registry-zookeeper.md)、[服务治理](./../08-governance.md)。
