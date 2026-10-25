# 测试

仓库只保留**一个测试模块** `tomato-rpc-test`：它把所有框架模块都以 `test` 作用域引进来，集中写测试。好处是依赖一次性配齐、测试代码不会散落在各模块；代价是「哪个模块缺测试」只能靠目录结构自己看。

## 运行

```bash
# 全量（会起内嵌 ZooKeeper + 真实 Netty 端口）
mvn test

# 只跑测试模块（先安装依赖模块）
mvn -pl tomato-rpc-test -am test

# 单个类 / 单个方法
mvn -pl tomato-rpc-test -am test -Dtest=RpcExecutionChainTest
mvn -pl tomato-rpc-test -am test -Dtest=RpcExecutionChainTest#directRpcTest

# 覆盖率报告（jacoco 已配在根 pom 的 pluginManagement 里）
mvn clean test jacoco:report
# 报告位置：tomato-rpc-test/target/coverage-reports
```

JaCoCo 在根 pom 里排除了 `org/tomato/study/rpc/sample/*`（样例代码不计入覆盖率），`report` 绑定在 `prepare-package` 阶段。

## 测试技术栈

| 依赖                                        | 用途                                                                       |
| ------------------------------------------- | -------------------------------------------------------------------------- |
| JUnit 4（`org.junit.Test` / `Assert`）      | 全部用例的基础框架，不是 JUnit 5                                           |
| Mockito                                     | `mock` / `spy` / `verify`，用于 invoker、server、`GrayRouterTestFacade` 等 |
| `spring-boot-starter-test` + `SpringRunner` | `@SpringBootTest` 装配测试（`TomatoRpcConfigurationTest`）                 |
| `curator-test` 的 `TestingServer`           | **进程内**起一个真 ZooKeeper，端口自定，无需外部环境                       |
| log4j2 + disruptor                          | 异步日志，避免测试输出阻塞                                                 |

所以这套测试**不依赖任何外部中间件**，`mvn test` 可以在干净机器上跑通；但它会占用固定端口（见下文「痛点」），不能并行跑多份。

## 分层

| 层     | 是否需要 ZK         | 是否需要网络   | 代表                                                                                                   |
| ------ | ------------------- | -------------- | ------------------------------------------------------------------------------------------------------ |
| 纯单元 | 否                  | 否             | `FenwickTreeTest`、`MetaDataTest`、`TokenLexerTest`、`ASTNodeParserTest`、`LoadBalanceTest`            |
| 组件   | 否                  | 否（Mock 掉）  | `BaseMicroServiceTest`、`DefaultCircuitBreakerTest`、`CompressInterceptorTest`、`CodecTest`            |
| 端到端 | 是（TestingServer） | 是（真实端口） | `RpcExecutionChainTest`、`NettyRpcServerTest`、`ZookeeperNameServerTest`、`TomatoRpcConfigurationTest` |

## 逐类清单

### common / core

| 测试                                                                                                                                   | 覆盖点                                                                                                                |
| -------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| [FenwickTreeTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/common/structure/FenwickTreeTest.java)                         | 树状数组前缀和，熔断计数窗口的底座                                                                                    |
| [UtilTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/common/utils/UtilTest.java)                                           | `NetworkUtil`（取本机 IP）、`ReflectUtils`                                                                            |
| [MetaDataTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/data/MetaDataTest.java)                                      | URI ↔ `MetaData` 互转、`equals`/`hashCode`、空值容错                                                                  |
| [RpcJvmConfigKeyTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/RpcJvmConfigKeyTest.java)                             | JVM 覆盖串 `a:1&b:2` 的解析                                                                                           |
| [SerializerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/serializer/SerializerTest.java)                            | protostuff 与 json 两种序列化的往返                                                                                   |
| [SpiLoaderTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/spi/SpiLoaderTest.java)                                     | 单例、setter 注入、**循环依赖**（`SpiInterfaceA/B/C/D` 互相依赖）、JVM 覆盖                                           |
| [LoadBalanceTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/loadbalance/LoadBalanceTest.java)                         | 平滑加权轮询：等权时严格 `1,2,3,1,2,3`；4:2:1 时的分配比例                                                            |
| [SuccessFailureRingCounterTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/circuit/SuccessFailureRingCounterTest.java) | 环形窗口计数、并发安全                                                                                                |
| [DefaultCircuitBreakerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/circuit/DefaultCircuitBreakerTest.java)         | 三态切换：错误率超阈值 → OPEN → 冷静期后 HALF_OPEN → 恢复 CLOSE                                                       |
| [BaseMicroServiceTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/router/BaseMicroServiceTest.java)                    | `refresh` 增删 invoker、`lookUp` 的 group 兜底与规则优先级（Mockito 驱动，`extends BaseTest` 复用 `mockMetadataSet`） |

### netty（网络通信，重点）

| 测试                                                                                                                            | 覆盖点                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [CodecTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/codec/CodecTest.java)                                   | `RawFrameEncoder` / `RawFrameDecoder`（帧层）编解码往返；`unpreparedTest` 只写入 24 字节帧头后断言 `readerIndex == 0`——**半包时一个字节都不消费**                                                                                                 |
| [NettyCodecTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/codec/NettyCodecTest.java)                         | 不用 `EmbeddedChannel`，而是真起一对 Netty 服务端/客户端（端口 `12580`），用 `CountDownLatch` 等 `bind` / `connect` 成功，验证 Pipeline 里 `FrameDecoder + ProtoDecoder + FrameEncoder` 组合的粘包/拆包                                           |
| [CompressInterceptorTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/interceptor/CompressInterceptorTest.java) | gzip 压缩的请求被 `interceptRequest` 就地解压还原；`postProcessResponse` 把响应体就地压缩并返回带扩展头 `2=true` 的新 `Command`                                                                                                                   |
| [NettyRpcServerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/transport/NettyRpcServerTest.java)             | ① `doubleStartTest`：4 个线程抢着 `init/start/stop`，CAS 保证只有一次真正生效（`verify(times(1))` + 线程池最终 shutdown）；② `idleTest`：读空闲超时后连接被服务端关闭，再写得到 `ClosedChannelException`；③ `keepAliveTest`：客户端心跳把连接续上 |
| [RpcExecutionChainTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/RpcExecutionChainTest.java)                 | 唯一一条**全链路**用例，见下                                                                                                                                                                                                                      |

`RpcExecutionChainTest` 用 `TestingServer(7642)` 起 ZK，然后拉起 4 个 `RpcCoreService`：

```text
prod 服务端      23456  microServiceId=DemoRpcServer        group=default
gray 服务端      23457  microServiceId=DemoRpcServer        group=gray
prod 下游        23458  microServiceId=DownstreamRpcServer  group=default
gray 下游        23459  microServiceId=DownstreamRpcServer  group=gray
客户端进程       23422  订阅 DemoRpcServer / DownstreamRpcServer，stage=dev
```

五个用例构成一条递进的学习路径：

| 用例                          | 验证什么                                                                                                                                                                                                                          |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `directRpcTest`               | 不经注册中心，`DirectStubInvoker` 直连某个 URI 就能调通                                                                                                                                                                           |
| `nameServerRouterRpcTest`     | 走 ZK 注册+订阅+兜底路由：客户端 `sum(numbers)` 与服务端直调结果一致（证明请求真的落到了同组实例并经过完整序列化/网络链路）                                                                                                       |
| `nameServerRouterGrayTest`    | 分两阶段：①规则 `USER_ID % 10 == 2` + `USER_ID=1` → 不命中 → 兜底走 **prod** 链路（断言端口 23422/23456/23458）；②把规则改成 `USER_ID % 10 == 1` 并用更大 opsId 重推 → 命中 → 整条链路切到 **gray**（断言端口 23422/23457/23459） |
| `directRpcTimeoutTest`        | 直连模式下调 `sleep(10s)` 的接口、超时设为 5s，断言抛出的 `TomatoRpcRuntimeException` 错误码是 `RPC_INVOCATION_TIMEOUT`（末尾再 `sleep(10s)` 才关服务，避免用例先于响应返回结束）                                                 |
| `nameServerRouterTimeoutTest` | 路由模式下同样断言 `RPC_INVOCATION_TIMEOUT`                                                                                                                                                                                       |

### registry / config / expression

| 测试                                                                                                                             | 覆盖点                                                                                                                                                                                                                                                |
| -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [ZookeeperNameServerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/registry/zookeeper/ZookeeperNameServerTest.java) | 注册 → 订阅 → 收到节点变更 → `refresh`；`TestingServer` 起在本机端口                                                                                                                                                                                  |
| [CuratorClientTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/registry/zookeeper/impl/CuratorClientTest.java)        | 已被 `//@Test` 注释掉，只留作连接真实 ZK 的手工脚本                                                                                                                                                                                                   |
| [TomatoRpcConfigurationTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/config/TomatoRpcConfigurationTest.java)       | `@SpringBootTest(classes = ...)` + `TestingServer(3689)` + [application-test.yaml](../tomato-rpc-test/src/test/resources/application-test.yaml)；断言 yaml 绑定、`@RpcClientStub` 字段注入、`@RpcServerStub` 注册、`circuit-open-rate` 的 `/100` 换算 |
| [TokenLexerTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/token/TokenLexerTest.java)                     | 词法切分，样例表达式集中在 [TestCode](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/TestCode.java) 的 `CODES` 列表                                                                                                                 |
| [ASTNodeParserTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/expression/ast/ASTNodeParserTest.java)                 | 优先级、左结合、括号、`->` 分叉、求值                                                                                                                                                                                                                 |

### 测试夹具

| 类                                                                                                                  | 作用                                                                                            |
| ------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| [core/base/BaseTest](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/core/base/BaseTest.java)                 | `mockMetadataSet(microServiceId)` 造 3 个不同 group 的实例；`checkInvokerMap` 比对 invoker 集合 |
| [TestService / TestServiceImpl](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/test/TestService.java)        | 基础样例接口（求和等）                                                                          |
| [TimeoutTest / TimeoutTestImpl](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/test/TimeoutTest.java)        | 故意 `sleep(10s)`，用来验证超时任务                                                             |
| [GrayRouterTestFacade / Impl](../tomato-rpc-test/src/test/java/org/tomato/study/rpc/test/GrayRouterTestFacade.java) | 灰度路由用例的承载接口，`setId` 区分 prod/gray 实例                                             |
| `config/test/*`                                                                                                     | `TestApi`、`TestClientBean`、`TestServerStub`、`TestTimeoutApi` 等注解装配夹具                  |

## 已知痛点

| #   | 问题                                                                   | 影响                                     |
| --- | ---------------------------------------------------------------------- | ---------------------------------------- |
| 1   | **端口全硬编码**（7642 / 23456～23459 / 39999 / 40000 / 40001 / 3689） | 同时跑两份测试或本机已有进程占用就会失败 |
| 2   | 依赖 `Thread.sleep` 等待注册与推送                                     | 机器慢时偶发失败，属于典型的 flaky 测试  |
| 3   | 端到端用例在 `@Before` 里串行拉起 4 个服务                             | 单个类跑一次要几秒到十几秒               |
| 4   | `CuratorClientTest` 被注释                                             | 该测试实际无覆盖                         |
| 5   | 没有 CI 配置                                                           | 只能本地手动跑，回归靠自觉               |
| 6   | `mvn test` 默认把端到端用例一起跑                                      | 只想跑单元测试时需要用 `-Dtest=` 挑类    |

## 补测试的建议姿势

- **纯逻辑优先**：`expression`、`loadbalance`、`circuit`、`metadata` 都是纯内存的，加断言成本最低。
- **需要 invoker 就打桩**：`RpcInvoker` 是接口，Mockito 一行 `when(invoker.getMetadata()).thenReturn(meta)` 就够（参考 `LoadBalanceTest`）。
- **需要 ZK 用 `TestingServer`**，不要连开发用的真实 ZK；记得在 `@After` 里 `testServer.close()`。
- **需要验证线程/生命周期**用 `spy` + `verify(times(n))`，参考 `doubleStartTest` 对 CAS 幂等的验证方式。
- 新增样例表达式时，直接往 `TestCode.CODES` 里加一条，`TokenLexerTest` / `ASTNodeParserTest` 就能顺带覆盖到。

相关章节：[网络通信·传输层](./06-transport.md)、[表达式引擎](./12-expression.md)、[部署与运维](./14-deployment.md)。
