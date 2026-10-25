# tomato-rpc-core

> 框架的「大脑」：所有抽象接口、数据模型、调用链骨架、路由与治理逻辑都在这里。它**不包含任何网络代码**，所以可以脱离 Netty 单独测试（`BaseMicroServiceTest`、`LoadBalanceTest`、`DefaultCircuitBreakerTest` 都是纯内存的）。

## 职责

| 它做什么                                                                               | 它不做什么                                 |
| -------------------------------------------------------------------------------------- | ------------------------------------------ |
| 定义 `RpcCoreService` 等 15 个核心接口                                                 | 具体跑在什么协议上（netty 模块）           |
| 动态代理 + 调用链编排（`BaseStubInvoker` → `RouterStubInvoker` → `CircuitRpcInvoker`） | 编解码、连接管理                           |
| 路由（`BaseMicroServiceSpace`、`ExprRouter`）与负载均衡                                | 注册中心的 ZK 细节                         |
| 熔断三态机与计数窗口                                                                   | Spring 装配                                |
| SPI 加载器、生命周期状态机                                                             | 序列化算法本身（调 protostuff/jackson 库） |
| 协议数据模型（`Command`/`Header`/`MetaData`）                                          | 字节布局（那只是常量，实现方在 netty）     |

## 依赖

```text
tomato-rpc-core
├── tomato-rpc-common      // Logger、NetworkUtil、FenwickTree、ApiUrl
└── tomato-rpc-expression  // 路由表达式的词法/语法/求值
```

## 包结构

| 包             | 内容                                                                                                                                                                                                                                                                                 |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `base/`        | `BaseRpcCoreService`——`RpcCoreService` 的骨架实现，串起 server/nameServer/microServiceSpace                                                                                                                                                                                          |
| `data/`        | 全部数据模型：`Command`、`Header`、`CommandType`、`ExtensionHeader`、`Invocation`、`InvocationContext`、`MetaData`、`RpcConfig`、`RpcServerConfig`、`StubConfig`、`Result`、`Response`、`RpcRequestDTO`、`RpcResponse`、`ProtoConstants`、`CommandFactory`、`ExtensionHeaderBuilder` |
| `stub/`        | `StubFactory`/`JdkStubFactory`、`StubInvoker`/`BaseStubInvoker`/`RouterStubInvoker`/`DirectStubInvoker`                                                                                                                                                                              |
| `invoker/`     | `RpcInvoker`（一个实例一条连接）、`BaseRpcInvoker`（优雅关闭）、`RpcInvokerFactory`                                                                                                                                                                                                  |
| `router/`      | `Router`、`MicroServiceSpace`、`BaseMicroServiceSpace`、`ExprRouter`、`FullRequestRouter`                                                                                                                                                                                            |
| `registry/`    | `NameServer`、`BaseNameServer`（队列串行刷新）、`NameServerFactory`                                                                                                                                                                                                                  |
| `server/`      | `RpcServer`、`BaseRpcServer`                                                                                                                                                                                                                                                         |
| `transport/`   | `RpcClient`、`BaseRpcClient`                                                                                                                                                                                                                                                         |
| `loadbalance/` | `LoadBalance`、`BaseLoadBalance`、`RoundRobinLoadBalance`（平滑加权）、`RandomLoadBalance`                                                                                                                                                                                           |
| `circuit/`     | `CircuitBreaker`、`CircuitStatus`、`DefaultCircuitBreaker`、`SuccessFailureRingCounter`、`CircuitRpcInvoker`、`CircuitBreakerFactory`                                                                                                                                                |
| `serializer/`  | `Serializer`、`ProtostuffSerializer`、`JsonSerializer`、`SerializerHolder`、`GzipWrapper`                                                                                                                                                                                            |
| `spi/`         | `SpiInterface`、`SpiLoader`                                                                                                                                                                                                                                                          |
| `observer/`    | `LifeCycle`、`LifeCycleEvent(Type)`、`LifeCycleListener`、`BaseLifeCycleComponent`                                                                                                                                                                                                   |
| `dashboard/`   | `RpcAppData`、`RpcInvokerData`、`RpcRouterData`、`RouterRefreshDTO`、`RpcRouterDTO`、`RpcInvokerModel`——监控接口与 dashboard 共用的数据结构                                                                                                                                          |
| `error/`       | `TomatoRpcErrorEnum`（错误码表）、`TomatoRpcException`（受检）、`TomatoRpcRuntimeException`                                                                                                                                                                                          |
| `io/`          | `StreamResource`、`FileStreamResource`、`UrlFileStreamResource`——SPI 配置文件的读取抽象                                                                                                                                                                                              |
| `utils/`       | `ClassUtil`（三级 ClassLoader 兜底）、`GzipUtils`                                                                                                                                                                                                                                    |
| 根包           | `RpcCoreService`、`RpcCoreServiceFactory`、`ProviderRegistry`、`DefalultProviderRegistry`、`ServerHandler`、`CommandInterceptor`、`ResponseFuture`、`RpcJvmConfigKey`                                                                                                                |

## 核心抽象

| 接口                           | 一句话                                                                     | 默认实现                                                                   |
| ------------------------------ | -------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `RpcCoreService`               | 门面：`init/start/stop`、`registerProvider`、`createStub`、`getNameServer` | `NettyRpcCoreService`（netty）                                             |
| `RpcCoreServiceFactory`        | 按 `RpcConfig` 造门面                                                      | `NettyRpcCoreServiceFactory`，SPI key `netty`                              |
| `NameServer`                   | 注册中心的抽象                                                             | `ZookeeperNameServer`                                                      |
| `NameServerFactory`            | 造 `NameServer`                                                            | `ZookeeperNameServerFactory`，key `zookeeper-factory`                      |
| `RpcServer`                    | RPC 服务端                                                                 | `NettyRpcServer`                                                           |
| `RpcClient`                    | 一条连接的客户端                                                           | `NettyRpcClient`                                                           |
| `ProviderRegistry`             | interface → 实例 的本地注册表                                              | `DefalultProviderRegistry`（类名少了个 `u`，是原样拼写）                   |
| `StubFactory`                  | 造动态代理                                                                 | `JdkStubFactory`，key `jdk`                                                |
| `StubInvoker`                  | 代理的 `InvocationHandler`                                                 | `RouterStubInvoker` / `DirectStubInvoker`                                  |
| `RpcInvoker`                   | 一个服务实例                                                               | `NettyRpcInvoker`                                                          |
| `LoadBalance`                  | 候选筛选后的选择                                                           | `RoundRobinLoadBalance`，key `weight-round-robin`                          |
| `Router` / `MicroServiceSpace` | 规则 / 一个微服务的实例集合                                                | `ExprRouter`/`FullRequestRouter` / `NettyMicroServiceSpace`                |
| `Serializer`                   | 序列化                                                                     | `JsonSerializer`（key `json`）、`ProtostuffSerializer`（key `protostuff`） |
| `CircuitBreaker`               | 熔断                                                                       | `DefaultCircuitBreaker`，key `default`                                     |
| `ServerHandler`                | 服务端某类命令的处理（JDK SPI）                                            | `RpcRequestHandler`、`KeepAliveRequestHandler`                             |
| `CommandInterceptor`           | 请求/响应的横切处理                                                        | `CompressInterceptor`                                                      |

## 启动与关闭

`BaseRpcCoreService` 把生命周期拆成三段，子类只需实现 `createXxx`：

```java
protected void doInit() {
    rpcServer.init();     // 建线程池、建 Bootstrap，不 bind
    nameServer.init();    // 连 ZK
}

protected void doStart() {
    rpcServer.start();                                    // 1. bind 端口
    nameServer.start();                                   // 2. 建会话
    nameServer.registerService(rpcServerMetaData);        // 3. 上报自身
    nameServer.subscribe(rpcServerMetaData, microServices, getStage());   // 4. 订阅下游
    ready = true;
}

protected void doStop() {
    nameServer.unsubscribe(microServices, stage);   // 先注销订阅
    nameServer.unregisterService(rpcServerMetaData);// 再注销自己
    for (MicroServiceSpace s : microServices) { s.close(); }
    rpcServer.stop();
    nameServer.stop();
}
```

顺序不能随便换：`doStart` 里 **先 `rpcServer.start()` 再 `registerService`**——注册的 URI 里要带真实端口；`doStop` 里 **先注销再关端口**，否则客户端可能连上一个正在死的实例。`doInit` 里 server 与 nameServer 的先后无所谓，两者没有依赖。

`getStage()` 里有一处 JVM 覆盖：`-Dtomato-rpc.service-stage=xxx` 优先于 `RpcConfig.stage`。

## 调用链骨架

```text
JdkStubFactory（JDK Proxy，InvocationHandler = StubInvoker）
└── RouterStubInvoker / DirectStubInvoker   ← BaseStubInvoker：保存/恢复 InvocationContext + MDC
    └── CircuitRpcInvoker                   ← 按 apiId 选断路器
        └── NettyRpcInvoker                 ← 组帧、注册超时
```

`BaseStubInvoker` 是最值得读的一个类：它把 `toString/hashCode/equals` 拦掉（否则会走网络）、保存「作为下游时的现场」、塞 `timeout`/`compress` 参数、把参数转成 `RpcRequestDTO`。详见 [调用链路](./../05-invocation.md)。

## 默认装配（SPI 配置文件）

| 文件（`src/main/resources/META-INF/tomato/`）     | 内容                                                               |
| ------------------------------------------------- | ------------------------------------------------------------------ |
| `...core.stub.StubFactory`                        | `jdk : ...JdkStubFactory`                                          |
| `...core.loadbalance.LoadBalance`                 | `weight-round-robin : ...RoundRobinLoadBalance`                    |
| `...core.circuit.CircuitBreakerFactory`           | `default : ...DefaultCircuitBreakerFactory`                        |
| `...core.serializer.Serializer`                   | `json : ...JsonSerializer`、`protostuff : ...ProtostuffSerializer` |
| `META-INF/services/...core.serializer.Serializer` | JDK SPI，供 `SerializerHolder` 用 ServiceLoader 加载全部实现       |

`Serializer` 同时出现在两处：**tomato SPI** 用于按 key 取单个实现，**JDK SPI** 用于枚举全部实现并按 `serializerIndex()` 建索引。

## 容易踩的点

| #   | 点                                                 | 说明                                                                                 |
| --- | -------------------------------------------------- | ------------------------------------------------------------------------------------ |
| 1   | `DefalultProviderRegistry`                         | 类名本身拼错（Defalult），`grep Default` 找不到它                                    |
| 2   | `BaseLifeCycleComponent` 的 `listeners` 从未被回调 | 状态机里定义了监听器集合，但 `LifeCycleEvent` 没有被真正 publish                     |
| 3   | `MetaData#equals` 忽略 `nodeProperty`              | 只改权重不会重建 invoker，权重变更不生效                                             |
| 4   | `MetaData#toMap` 只有 `group`、`weight`            | 表达式路由在实例侧只能看到这两个变量                                                 |
| 5   | `RpcConfig` 是 `record`                            | 加字段要改构造器，`RpcConfig.Builder` 的默认值集中在一处，容易漏                     |
| 6   | `RpcJvmConfigKey` 用 `&` 和 `:` 做分隔符           | 与扩展头的 `k:v&k:v` 同款格式，但两者完全独立                                        |
| 7   | `GzipWrapper` 是装饰器                             | 可以对任意 `Serializer` 再包一层 gzip，但实际压缩走的是 `CompressInterceptor` 而非它 |
| 8   | `core/dashboard/*` 同时被 config 和 dashboard 使用 | 改这些 DTO 会影响两个应用，注意兼容                                                  |

## 关键类速查

| 类                         | 位置                                                                                                             | 一句话                           |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| `RpcCoreService`           | [core](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/RpcCoreService.java)                        | 门面接口                         |
| `BaseRpcCoreService`       | [core/base](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/base/BaseRpcCoreService.java)          | 门面骨架 + 生命周期编排          |
| `DefalultProviderRegistry` | [core](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/DefalultProviderRegistry.java)              | 本地 provider 注册表             |
| `BaseStubInvoker`          | [core/stub](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/stub/BaseStubInvoker.java)             | 代理到网络调用的转折点           |
| `BaseMicroServiceSpace`    | [core/router](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/router/BaseMicroServiceSpace.java)   | 路由筛选 + 实例刷新 + 表达式编译 |
| `BaseRpcInvoker`           | [core/invoker](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/invoker/BaseRpcInvoker.java)        | 引用计数 + 60s 优雅关闭          |
| `BaseNameServer`           | [core/registry](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/registry/BaseNameServer.java)      | 变更队列串行刷新                 |
| `SpiLoader`                | [core/spi](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/spi/SpiLoader.java)                     | 单例 + setter 注入 + JVM 覆盖    |
| `DefaultCircuitBreaker`    | [core/circuit](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/circuit/DefaultCircuitBreaker.java) | 三态机                           |
| `TomatoRpcErrorEnum`       | [core/error](../../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/error/TomatoRpcErrorEnum.java)        | 全部错误码                       |

配套章节：[架构分层](./../03-architecture.md)、[调用链路](./../05-invocation.md)、[服务治理](./../08-governance.md)、[SPI 与生命周期](./../09-spi-lifecycle.md)。
