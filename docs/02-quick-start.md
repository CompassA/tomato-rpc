# 快速开始

本章只讲「怎么把它跑起来」。原理细节见 [总览](./01-overview.md) 之后的三篇网络通信文档。

## 前置条件

| 依赖      | 版本  | 说明                                 |
| --------- | ----- | ------------------------------------ |
| JDK       | 17+   | 根 `pom.xml` 的 `java.version` 为 17 |
| Maven     | 3.6+  | 需要能访问中央仓库                   |
| ZooKeeper | 3.5.x | 默认连接 `127.0.0.1:2181`            |

```bash
# 启动一个单机 ZK
docker run -d --name tomato-zk -p 2181:2181 zookeeper:3.5.9
```

## 构建

```bash
mvn -q clean install -DskipTests
```

构建产物全部安装到本地仓库，`tomato-rpc-spring-boot-starter` 是业务方应该引入的唯一坐标：

```xml
<dependency>
    <groupId>org.tomato.study.rpc</groupId>
    <artifactId>tomato-rpc-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 方式一：手动接入（不依赖 Spring）

对应样例 [tomato-rpc-sample-server](../tomato-rpc-sample-server/src/main/java/org/tomato/study/rpc/sample/server/DemoServerApplication.java) 与 [tomato-rpc-sample-client](../tomato-rpc-sample-client/src/main/java/org/tomato/study/rpc/sample/client/DemoClientApplication.java)。

### 服务端

```java
RpcConfig config = RpcConfig.builder()
        .microServiceId("demo-rpc-service")
        .nameServiceURI("127.0.0.1:2181")
        .port(7777)
        .stage("dev")
        .group("main")
        .build();

RpcCoreService coreService = SpiLoader.getLoader(RpcCoreServiceFactory.class)
        .load()
        .create(config);
coreService.registerProvider(new EchoServiceImpl(), EchoService.class);
coreService.init();
coreService.start();
```

`RpcCoreServiceFactory` 通过 SPI 加载，默认实现是 `netty`（`META-INF/tomato/org.tomato.study.rpc.core.RpcCoreServiceFactory`）。

### 客户端

```java
RpcConfig config = RpcConfig.builder()
        .microServiceId("demo-rpc-client")
        .subscribedServiceIds(Collections.singletonList("demo-rpc-service"))
        .nameServiceURI("127.0.0.1:2181")
        .port(7890)          // 客户端自身也会起一个 server，可选但建议填
        .build();

RpcCoreService coreService = SpiLoader.getLoader(RpcCoreServiceFactory.class)
        .load()
        .create(config);
coreService.init();
coreService.start();

StubConfig<EchoService> stubConfig = new StubConfig<>(
        EchoService.class,          // 接口
        "demo-rpc-service",         // 目标 micro-service-id
        coreService.getGroup(),     // 目标 group，空串表示"与我同组"
        false,                      // 是否压缩请求体
        5000L,                      // 超时 ms
        coreService.getNameServer() // 名字服务
);
EchoService stub = coreService.createStub(stubConfig);

DemoResponse response = stub.echo(new DemoRequest("hello world"));
```

> 客户端也注册了 `microServiceId`，因为 `RpcCoreService` 同时具备「客户端」和「服务端」两种身份：订阅别人、同时也能被注册与发现（`ExecutionChainFacade` 的调用链演示就依赖这一点）。

## 方式二：Spring Boot 接入

对应样例 [tomato-rpc-spring-sample-server](../tomato-rpc-spring-sample-server/src/main/resources/application.yaml) 与 [tomato-rpc-spring-sample-client](../tomato-rpc-spring-sample-client/src/main/resources/application.yaml)。

### 1. 配置

```yaml
server:
  port: 9090 # 普通 HTTP 端口
tomato-rpc:
  micro-service-id: "demo-rpc-service"
  subscribed-services: # 需要订阅的下游服务
    - "demo-downstream-rpc-service"
  name-service-uri: "127.0.0.1:2181"
  port: 4567 # RPC 端口
  business-thread: 100 # >1 时开启业务线程池
  stage: "dev"
  group: "main"
  server-idle-check-ms: 600000
  client-keep-alive-ms: 200000
  enable-circuit: true
  circuit-open-rate: 50 # 百分比，内部换算为 0.5
  circuit-open-seconds: 60
  circuit-window: 100
```

完整字段语义见 [Spring 集成](./10-spring.md#配置项)。注意 `use-gzip` 这类属性在当前 `TomatoRpcProperties` 中**并不存在**，压缩由 `@RpcClientStub(compressBody = true)` 控制。

### 2. 暴露服务

```java
@RpcServerStub                    // = @Component + 注册为 provider
@AllArgsConstructor
public class EchoServiceImpl implements EchoService, SumService {
    @Override
    public DemoResponse echo(DemoRequest request) { ... }
}
```

一个类实现多个接口时，用 `@RpcServerStub(interfaceTypes = {EchoService.class})` 精确指定要注册哪个接口。

### 3. 引用服务

```java
@RpcClientStub(microServiceId = "demo-rpc-service", timeout = 1000, compressBody = true)
private EchoService echoService;

@RpcClientStub(microServiceId = "demo-rpc-service", group = "gray")
private SumService graySumService;
```

字段注入由 `RpcStubPostProcessor` 完成，代理对象在 Bean 初始化后写入字段。

### 4. 启动类

```java
@SpringBootApplication
public class SpringDemoServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringDemoServerApplication.class);
    }
}
```

框架在 `ContextRefreshedEvent` 中调用 `init()` + `start()`，在 `ContextClosedEvent` 中调用 `stop()`，业务代码无需关心生命周期（见 [Spring 集成](./10-spring.md#生命周期挂载点)）。

## 验证是否成功

1. 服务端日志出现 `netty rpc core service started. micro-service-id=...,stage=...,group=...,host=...,port=...`
2. ZooKeeper 上出现节点：

   ```bash
   docker exec -it tomato-zk zkCli.sh -server 127.0.0.1:2181
   ls /tomato/demo-rpc-service/dev/providers
   ```

   节点名为 URL 编码后的实例地址，形如 `tomato%3A%2F%2F192.168.1.5%3A4567%2F%3Fmicro-service-id%3D...`。

3. 客户端调用返回并打印服务端实例信息（样例的 `EchoServiceImpl` 会把 `host/port/stage/group` 拼进返回值，便于肉眼确认打到了哪个实例）。

## 常见问题

| 现象                                   | 原因                                                    | 处理                                       |
| -------------------------------------- | ------------------------------------------------------- | ------------------------------------------ |
| 启动报 `missing micro-service-id`      | 未配置 `micro-service-id`                               | 补配置                                     |
| 调用报 `invoker not found`             | 订阅了但实例列表为空 / stage 不一致                     | 检查 `subscribed-services` 与两端 `stage`  |
| 调用报 `client rpc invocation timeout` | 超时时间太短或服务端耗时过长                            | 调大 `@RpcClientStub(timeout)`             |
| 连接不上注册中心                       | ZK 未启动或 `name-service-uri` 写错                     | `echo ruok \| nc 127.0.0.1 2181`           |
| 灰度流量打到了主分组                   | 客户端 `group` 与服务端 `group` 不同，会走兜底路由      | 见 [服务治理](./08-governance.md#兜底路由) |
| 端口冲突                               | 样例里 `server.port` 与 `tomato-rpc.port` 都可能是 9090 | 明确区分 HTTP 端口与 RPC 端口              |

## 下一步

- 想看一次调用在网络上长什么样 → [通信协议](./04-protocol.md)
- 想改框架行为（换序列化、换负载均衡、加拦截） → [SPI 与生命周期](./09-spi-lifecycle.md)
- 想用 docker / k8s 跑 → [部署样例](./14-deployment.md)
