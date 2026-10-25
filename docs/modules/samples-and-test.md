# 样例与测试

> 这一篇覆盖两类「非框架核心」的模块：5 个可运行的示例工程（`tomato-rpc-sample-*`、`tomato-rpc-spring-sample-*`）和 1 个测试模块（`tomato-rpc-test`），外加一个只放日志配置的 `tomato-rpc-log4j2-config`。

## 模块一览

| 模块                              | 类型   | 一句话                                                                           |
| --------------------------------- | ------ | -------------------------------------------------------------------------------- |
| `tomato-rpc-sample-api`           | 库     | 共享的接口与 DTO（`EchoService`、`SumService`、`ExecutionChainFacade`）          |
| `tomato-rpc-sample-server`        | 可执行 | 手写 SPI 装配的服务端，端口 4567，单接口 `EchoService`                           |
| `tomato-rpc-sample-client`        | 可执行 | 手写 SPI 装配的客户端，端口 7890，含压测模式                                     |
| `tomato-rpc-spring-sample-server` | 可执行 | Spring Boot 服务端，默认 profile 下 13333（HTTP）/ 4567（RPC），可切换成下游服务 |
| `tomato-rpc-spring-sample-client` | 可执行 | Spring Boot 客户端，9090/4567，三种调用模式                                      |
| `tomato-rpc-test`                 | 测试   | 全部单测/集成测试（jar 打包，只在 test 阶段跑）                                  |
| `tomato-rpc-log4j2-config`        | 资源   | 只有一份 `log4j2.xml`，给需要统一日志格式的地方复用                              |

## 共享 API

| 接口                   | 方法                                                             | 说明                                                                                       |
| ---------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `EchoService`          | `DemoResponse echo(DemoRequest request)`                         | 返回「服务端看到的 host / microServiceId / stage / group」，用来肉眼确认请求落到了哪个实例 |
| `SumService`           | `Integer sum(List<Integer> nums)`                                | 用于 Spring 客户端的 `sum` 模式                                                            |
| `ExecutionChainFacade` | `ExecutionChainResponse dispatch(ExecutionChainRequest request)` | 每跳把自身 URL 追加进列表，用来观察灰度路由把请求送到了哪几个实例                          |

| DTO                                  | 关键字段                                                                                    |
| ------------------------------------ | ------------------------------------------------------------------------------------------- |
| `DemoRequest`                        | `String data` + 一个 `final` 的 `TestDTO`（验证嵌套对象序列化）                             |
| `DemoResponse`                       | `public String data`（字段直接 public，序列化器都能处理）                                   |
| `ExecutionChainRequest` / `Response` | `List<String> serverURLList`                                                                |
| `Constant`                           | `serviceId = "demo-rpc-service"`、`testDownStreamServiceId = "demo-downstream-rpc-service"` |

## 非 Spring 样例

### 服务端

```java
RpcCoreService coreService = SpiLoader.getLoader(RpcCoreServiceFactory.class).load()
    .create(RpcConfig.builder()
        .microServiceId(Constant.serviceId)
        .nameServiceURI(zkURL)          // 默认 127.0.0.1:2181，可被 -Dtomato-rpc.name-service-uri 覆盖
        .port(4567)
        .businessThreadPoolSize(4)
        .build());
coreService.registerProvider(new EchoServiceImpl(coreService), EchoService.class);
coreService.init();
coreService.start();
Runtime.getRuntime().addShutdownHook(new Thread(coreService::stop));
```

没有 yaml、没有 Spring：`META-INF/tomato/org.tomato.study.rpc.core.RpcCoreServiceFactory` 里写 `netty : ...NettyRpcCoreServiceFactory`，`SpiLoader.load()` 就拿到 Netty 实现。

### 客户端

```java
RpcConfig config = RpcConfig.builder()
    .microServiceId("demo-rpc-client")
    .subscribedServiceIds(Collections.singletonList(Constant.serviceId))
    .nameServiceURI(zkURL)
    .port(7890)                 // 客户端也要起服务端！它自身可被调用，也用于注册元数据
    .build();
rpcCoreService.init();
rpcCoreService.start();

EchoService stub = rpcCoreService.createStub(new StubConfig<>(
    EchoService.class, Constant.serviceId, rpcCoreService.getGroup(), false, 5000L,
    rpcCoreService.getNameServer()));
```

两种跑法：

| 环境变量    | 行为                                                                             |
| ----------- | -------------------------------------------------------------------------------- |
| 不设 `PERF` | 10 线程 × 1000 次，每次调用后 `sleep(1500)`，日志逐条打印                        |
| `PERF=1`    | 50 线程 × 1000 次，body 约 37 KB（1000 个 UUID 拼接），最后打印总耗时与 QPS 估算 |

## Spring 样例

### 服务端

`ExecutionChainFacadeImpl` 同时是「服务提供者」和「服务调用者」：

```java
@RpcServerStub
public class ExecutionChainFacadeImpl implements ExecutionChainFacade {
    @RpcClientStub(microServiceId = Constant.testDownStreamServiceId)
    private ExecutionChainFacade downstreamFacade;

    public ExecutionChainResponse dispatch(ExecutionChainRequest request) {
        // 把自己加进列表；如果自己不是下游服务，就继续调下游
        if (!Objects.equals(metaData.getMicroServiceId(), Constant.testDownStreamServiceId)) {
            return downstreamFacade.dispatch(downstreamRequest);
        }
        return response;
    }
}
```

profile 决定它是「上游」还是「下游」：

| profile                      | server.port | RPC port | micro-service-id              | stage | group |
| ---------------------------- | ----------- | -------- | ----------------------------- | ----- | ----- |
| 默认（`application.yaml`）   | 9090        | 4567     | `demo-rpc-service`            | dev   | main  |
| `downstrem-prod`（注意拼写） | 9090        | 4567     | `demo-downstream-rpc-service` | prod  | main  |
| `downstream-gray`            | 9090        | 4567     | `demo-downstream-rpc-service` | prod  | gray  |
| `downstream-dev`             | 13334       | 4568     | `demo-downstream-rpc-service` | dev   | main  |
| `default`                    | 13333       | 4567     | 默认                          | dev   | main  |
| `gray`                       | 9090        | 4567     | 默认                          | prod  | gray  |
| `prod`                       | 9090        | 4567     | 默认                          | prod  | main  |

灰度实验就是「同机起多个 profile、用不同 group 区分」，`application-downstrem-prod.yaml` 的文件名少了个 `e`，`--spring.profiles.active=downstrem-prod` 时要照抄这个拼写。

### 客户端

```java
@RpcClientStub(microServiceId = Constant.serviceId, compressBody = true, timeout = 1000)
private EchoService echoService;

@RpcClientStub(microServiceId = Constant.serviceId, compressBody = true, timeout = 5000)
private SumService sumService;

@RpcClientStub(microServiceId = Constant.serviceId)
private ExecutionChainFacade executionChainFacade;
```

`-Dtomato-rpc.client-mode` 决定跑哪条路径：

| 模式            | 行为                                                                                        |
| --------------- | ------------------------------------------------------------------------------------------- |
| 不设 / `router` | 每轮 `InvocationContext.put("USER_ID", "2323232321002")`，调 `dispatch`，打印链路经过的实例 |
| `echo`          | 调 `echo`，验证超时 1000ms + 压缩                                                           |
| `sum`           | 调 `sum`，验证集合参数序列化                                                                |

主循环是 12 个线程 × 1,000,000 次（几乎等于「一直跑到 Ctrl-C」），每次调用后随机 sleep 0~5s，并手工维护 `InvocationContext` 与 `MDC`：

```java
InvocationContext.initContext();
MDC.put(ExtensionHeader.TRACE_ID.name(), ExtensionHeader.TRACE_ID.getValueFromContext());
try { runnable.run(); } finally { InvocationContext.remove(); MDC.clear(); }
```

这是使用 traceId 的**正确示范**：线程复用场景必须 `finally` 清理，否则下一个请求会带上上一个的 traceId。

## 测试模块

`tomato-rpc-test` 是唯一的测试模块（其他模块 `src/test` 下没有用例），按「是否需要外部资源」分三层：

| 层            | 用例                                                                                                                                                                                                                                                                                | 需要什么              |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| 纯内存        | `FenwickTreeTest`、`UtilTest`、`MetaDataTest`、`LoadBalanceTest`、`DefaultCircuitBreakerTest`、`SuccessFailureRingCounterTest`、`SerializerTest`、`SpiLoaderTest`、`RpcJvmConfigKeyTest`、`UrlFileStreamResourceTest`、`BaseMicroServiceTest`                                       | 无                    |
| 真实网络/端口 | `CodecTest`、`NettyCodecTest`（12580）、`NettyRpcServerTest`、`CompressInterceptorTest`、`RpcExecutionChainTest`（23422 / 23456~23459）、`ZookeeperNameServerTest`（5422）、`CuratorClientTest`、`TomatoRpcConfigurationTest`（3689 + 7854）、`ASTNodeParserTest`、`TokenLexerTest` | 端口、`TestingServer` |
| 夹具          | `TestService`/`Impl`、`TimeoutTest`/`Impl`、`GrayRouterTestFacade`/`Impl`、`config/test/*`、`spi/spitest/*`                                                                                                                                                                         | 被上面的用例引用      |

```bash
# 只跑测试模块
mvn -pl tomato-rpc-test -am test

# 单个用例
mvn -pl tomato-rpc-test -Dtest=RpcExecutionChainTest test
```

测试细节（每个用例断言什么、灰度两阶段怎么切端口）见 [测试](./../13-testing.md)。

## 常见坑

| #   | 坑                                          | 说明                                                              |
| --- | ------------------------------------------- | ----------------------------------------------------------------- |
| 1   | 同机起 Spring 服务端 + 客户端会抢端口       | 两个样例都监听 HTTP 9090 和 RPC 4567，必须换 profile 或改 yaml    |
| 2   | 非 Spring 客户端也要占端口                  | `port(7890)` 不是可选项，`init/start` 会真的 bind                 |
| 3   | `-Dtomato-rpc.client-mode` 拼错就是默认模式 | 空值也会走 `router` 分支，看起来「配置没生效」                    |
| 4   | 大日志量                                    | 非 Spring 客户端 10 线程各打 1000 行；perf 模式不打日志但压满 CPU |
| 5   | 测试依赖固定端口                            | 并行跑多个测试类会端口冲突，CI 上要串行或改端口                   |
| 6   | `TestCode` 是 main 不是测试                 | 它没有 `@Test`，`mvn test` 不会执行                               |
| 7   | 测试用例会真起 ZK                           | `TestingServer` 每次新建临时目录，机器上要能写 `/tmp`             |

## 阅读顺序

1. [tomato-rpc-sample-api](../../tomato-rpc-sample-api/src/main/java/org/tomato/study/rpc/sample/api/EchoService.java)：三个接口，先知道被测的是什么。
2. [DemoServerApplication](../../tomato-rpc-sample-server/src/main/java/org/tomato/study/rpc/sample/server/DemoServerApplication.java) → [DemoClientApplication](../../tomato-rpc-sample-client/src/main/java/org/tomato/study/rpc/sample/client/DemoClientApplication.java)：不用 Spring 的最小闭环。
3. [SpringDemoClientApplication](../../tomato-rpc-spring-sample-client/src/main/java/org/tomato/study/rpc/sample/spring/client/SpringDemoClientApplication.java)：注解注入、traceId、灰度验证。
4. [RpcExecutionChainTest](../../tomato-rpc-test/src/test/java/org/tomato/study/rpc/netty/RpcExecutionChainTest.java)：把整条链路当黑盒跑一遍。

配套章节：[快速开始](./../02-quick-start.md)、[测试](./../13-testing.md)、[部署](./../14-deployment.md)。
