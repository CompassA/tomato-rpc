# tomato-rpc-dashboard

> 一个独立的 Spring Boot 应用（不是库）：运维通过它的 HTTP 接口查看集群实例、查看/修改路由规则；它把规则写进 ZK，再逐个实例 HTTP 推送刷新。**没有前端页面**，只有 REST 接口。

端口 `12222`，依赖 `tomato-rpc-core` + `tomato-rpc-registry-zookeeper` + Apache HttpClient 5，自己 new 了一套 Curator（不复用 registry 模块里的 `CuratorClient`）。

## 职责

| 它做什么                                  | 它不做什么                                     |
| ----------------------------------------- | ---------------------------------------------- |
| 列出某服务在某环境下注册的实例            | 采集指标（没有 QPS/延迟/错误率上报）           |
| 读取 ZK 中「调用方 → 目标服务」的路由规则 | 解析表达式（实例侧解析，dashboard 只存字符串） |
| 修改规则：写 ZK + 推送每个实例            | 重试失败的推送                                 |
| 统一错误码与请求/响应日志                 | 鉴权、审计、多租户                             |

## 类清单

| 类                                                                                                                                                     | 职责                                                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| [DashboardApplication](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/DashboardApplication.java)                              | `@SpringBootApplication` 启动类                                                                            |
| [RpcDashboardController](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/web/RpcDashboardController.java)                      | 三个 REST 接口                                                                                             |
| `ApiPath`                                                                                                                                              | 接口路径常量（**与 `common` 的 `ApiUrl` 是两套**：这里是 dashboard 自己的对外接口）                        |
| [RpcDashboardServiceImpl](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/service/impl/RpcDashboardServiceImpl.java)           | 业务层，三个方法纯转发                                                                                     |
| `RpcDashboardService` / `ListInvokerReq` / `ListRouterReq` / `RpcRouterModifyRequest`                                                                  | 服务接口与请求对象                                                                                         |
| [NameServerManager](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/dao/NameServerManager.java)                                | DAO 的一层壳（`record` 委托），便于以后换实现                                                              |
| [ZookeeperRegistryDAO](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/dao/ZookeeperRegistryDAO.java)                          | 真正干活：查实例、读写路由、加锁、HTTP 通知                                                                |
| `NameServerDAO`                                                                                                                                        | DAO 接口                                                                                                   |
| [RpcInvokerAssembler](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/assembler/RpcInvokerAssembler.java)                      | `MetaData` ↔ `RpcInvokerData`、`RpcRouterData` ↔ `RpcRouterDTO` 的转换                                     |
| [TomatoRpcDashboardConfiguration](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/config/TomatoRpcDashboardConfiguration.java) | 装配 Curator、HttpClient、通知线程池、DAO                                                                  |
| `DashboardProperties`                                                                                                                                  | 唯一配置项 `tomato-rpc-dashboard.name-server-url`                                                          |
| `ServiceTemplate` / `ServiceTemplateEnum`                                                                                                              | 统一的请求日志 + 异常 → 错误码 模板方法                                                                    |
| `exception/*`                                                                                                                                          | `DashboardParamException`（参数）、`DashboardRuntimeException`（业务）、`DashboardSystemException`（系统） |
| `DashboardResponse` / `ResponseCodeEnum`                                                                                                               | 响应外壳与错误码（10000 成功 / 10001 参数 / 10002 未知 / 20001 ZK 错误）                                   |

## 三个接口

| 方法 | 路径                            | 参数                                              | 返回 `data`                          |
| ---- | ------------------------------- | ------------------------------------------------- | ------------------------------------ |
| GET  | `/api/tomato/stat/invokers`     | `microServiceId`、`stage`                         | `{microServiceId, invokers:[{url}]}` |
| GET  | `/api/tomato/stat/routers`      | `microServiceId`、`stage`、`routerMicroServiceId` | `[{priority, expr}]`                 |
| POST | `/api/tomato/ops/router/modify` | body：`RpcRouterModifyRequest`                    | `null`（成功即 `code=10000`）        |

注意两个 id 的方向：**`microServiceId` 是规则持有方（调用方），`routerMicroServiceId` 是被调方**。因为规则挂在 `/tomato/{调用方}/{stage}/routers/{被调方}` 上。

请求体示例：

```json
{
  "microServiceId": "demo-rpc-client",
  "stage": "dev",
  "routerMicroServiceId": "demo-rpc-service",
  "exprList": [
    { "priority": 1, "expr": "USER_ID % 10 == 2 -> group == \"gray\"" },
    { "priority": 2, "expr": "1 -> group == \"main\"" }
  ]
}
```

`exprList` 会按 `priority` 升序排序、只取 `expr` 字符串写进 ZK（`RpcRouterData` 里没有 priority 字段，顺序即优先级）。

## 下发链路

```text
POST /api/tomato/ops/router/modify
  RpcDashboardController
    └── ServiceTemplate#process（打请求日志、统一异常）
        └── RpcDashboardServiceImpl#routerModify
            ├── RpcInvokerAssembler.toRouterData(exprList)     // 排序 + 只留表达式
            └── ZookeeperRegistryDAO#saveRoutersAndNotify
                ├── InterProcessMutex("/lock").acquire(10s)
                │     opsGlobalId = System.currentTimeMillis()
                │     Thread.sleep(1)                          // 避免同毫秒
                ├── doRoutersSave：ZK 里没有就建 PERSISTENT，然后 setData(JSON)
                └── 对 listInvokers(microServiceId, stage) 的每个实例
                      refreshRouterExecutor.execute(...)
                        POST http://{host}:9090/api/tomato/router/local/refresh
```

关键点：

- `opsGlobalId` **只存在于这次 HTTP 请求里**，ZK 中只有 `{"expr":[...]}`；实例重启后靠「读一次 ZK」恢复，此时用 `INITIAL_ROUTER_OPS_ID = 0`。
- 推送目标端口写死 `9090`（业务应用的 Spring HTTP 端口），不是 RPC 端口。
- 通知线程池：`core 5 / max 20 / 队列 5000 / 拒绝时打 error 后抛 AbortPolicy`。
- HttpClient：`maxConnTotal = 1000` 但 `maxConnPerRoute = 1`，对同一个实例的推送是串行的。

## 为什么用模板方法

`ServiceTemplate#process` 把三件事收拢在一处：

```java
try {
    validate(req);
    R res = doProcess(req);
    return DashboardResponse.success(res);
} catch (AbstractDashboardException e) {
    // 参数错 → warn + 原始 errMsg；系统错 → error + 固定 "system error"
} catch (Throwable e) {
    return DashboardResponse.failed(ResponseCodeEnum.RPC_UNKNOWN_EXCEPTION);
} finally {
    log.info("|resp|{}|{}|{}|{}|{}|", module, method, resMark, cost, resp);
}
```

所以控制器里的 `doProcess` 只需要写业务，不用关心错误码；`module` 来自 `ServiceTemplateEnum`（`RpcStatus` / `RpcOps`），日志里能直接区分「看」还是「改」。

## 与实例侧的契约

| dashboard 侧                                                                                                                   | 实例侧（config 模块）                                   |
| ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------- |
| 读实例列表：`ZookeeperAssembler.buildServiceNodeParent(id, stage)` + `getChildren`                                             | 注册：`buildServiceNodePath(...)` 写 EPHEMERAL          |
| 写规则：`buildServiceRouterPath(caller, stage, target)`                                                                        | 启动时读同一个路径                                      |
| 推送：`POST http://host:9090` + `RouterRefreshDTO`                                                                             | `MonitorController#routerRefresh` 接 `RouterRefreshDTO` |
| 共用常量：`ApiUrl.ROUTER_REFRESH`、`ZookeeperAssembler`、`RpcRouterData`/`RouterRefreshDTO`（都在 core，为了让两边不重复定义） |

## 配置

```yaml
spring:
  application:
    name: "tomato-rpc-dashboard-application"
tomato-rpc-dashboard:
  name-server-url: "127.0.0.1:2181"
server:
  port: 12222
```

`name-server-url` 可以被 `-Dtomato-rpc.name-service-uri` 覆盖（与业务应用同一个 JVM key）。

## 已知问题

| #   | 问题                                                | 影响                                                                            |
| --- | --------------------------------------------------- | ------------------------------------------------------------------------------- |
| 1   | 推送端口硬编码 `9090`                               | 业务应用换了 HTTP 端口（如 spring 样例的 13333/13334）就永远收不到推送          |
| 2   | 锁只包住「生成 opsId」                              | 两个 dashboard 并发下发时，写 ZK 与推送的先后仍可能乱序，实例会拒绝较小的 opsId |
| 3   | 推送失败只打日志、无重试                            | 实例只能等重启时从 ZK 读一次，期间一直是旧规则                                  |
| 4   | 不校验表达式语法                                    | 非法规则会写进 ZK，所有实例刷新失败（且 opsId 被推进），只能重新下发正确规则    |
| 5   | `opsGlobalId` 用毫秒时间戳                          | 机器时钟回拨会让新规则被判定为「旧版本」                                        |
| 6   | 没有鉴权与操作审计                                  | 能访问 12222 就能改线上路由                                                     |
| 7   | 没有前端                                            | 只能 curl / Postman，或自己接一层 UI                                            |
| 8   | `routerList` 对不存在的规则直接抛 ZK 异常           | 返回 20001 而不是空列表，前端需要额外处理                                       |
| 9   | Curator 用 `ExponentialBackoffRetry(1000, 1000000)` | 与业务侧 `(1000, 3)` 不一致，ZK 长时间不可用时这个线程会一直重试                |

## 本地跑一遍

```bash
# 1. 起 ZK（本地 2181）
# 2. 起 dashboard
mvn -pl tomato-rpc-dashboard -am spring-boot:run
# 3. 看实例
curl "http://127.0.0.1:12222/api/tomato/stat/invokers?microServiceId=demo-rpc-service&stage=dev"
# 4. 下发一条恒真规则
curl -X POST http://127.0.0.1:12222/api/tomato/ops/router/modify \
  -H 'Content-Type: application/json' \
  -d '{"microServiceId":"demo-rpc-service","stage":"dev","routerMicroServiceId":"demo-downstream-rpc-service","exprList":[{"priority":1,"expr":"1 -> group == \"main\""}]}'
```

第 4 步如果实例的 HTTP 端口不是 9090，接口会返回成功但实例不会更新——这是最容易误判的一次「成功」。

## 阅读顺序

1. [RpcDashboardController](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/web/RpcDashboardController.java)：三个接口。
2. [ZookeeperRegistryDAO](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/dao/ZookeeperRegistryDAO.java)：读写 ZK + 推送。
3. [RpcInvokerAssembler](../../tomato-rpc-dashboard/src/main/java/org/tomato/study/rpc/dashboard/assembler/RpcInvokerAssembler.java)：DTO 与存储格式的转换。
4. `ServiceTemplate`：错误码与日志的统一入口。

配套章节：[监控与运营](./../11-dashboard.md)、[服务治理](./../08-governance.md)、[表达式引擎](./../12-expression.md)。
