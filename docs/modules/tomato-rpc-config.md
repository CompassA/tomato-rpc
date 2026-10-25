# tomato-rpc-config

> Spring / Spring Boot 装配模块：把 `application.yaml` 变成 `RpcConfig`，把 `@RpcClientStub` 字段变成动态代理，把 `@RpcServerStub` 类注册成本地 provider，并顺带暴露几个本机监控接口。

## 职责

| 它做什么                                                          | 它不做什么                               |
| ----------------------------------------------------------------- | ---------------------------------------- |
| 自动装配入口（`spring.factories` + `AutoConfiguration.imports`）  | 实现 RPC 本身（core / netty）            |
| 读取 `tomato-rpc.*` 配置，构造 `RpcConfig`                        | 提供远程监控（dashboard 是独立应用）     |
| `@RpcClientStub` 注入 + `@RpcServerStub` 注册                     | 管理注册中心（registry-zookeeper）       |
| 用 `ApplicationEvent` 驱动 core 的 `init/start/stop`              | 决定路由规则（dashboard / 运维手工下发） |
| 暴露 `/api/tomato/invoker/*`、`/api/tomato/router/*` 四个本地接口 | 做前端（本项目没有 Web UI）              |

## 依赖

```text
tomato-rpc-config
├── tomato-rpc-core      // RpcCoreService、RpcConfig、StubConfig
└── tomato-rpc-netty     // 默认实现；config 自己声明了 SPI 文件把它绑进来
```

**config 主动依赖 netty**，这是与「netty 依赖 registry-zookeeper」配对的另一半：所以最终的 `tomato-rpc-spring-boot-starter` 只声明一个 `tomato-rpc-config` 依赖，就拿到了 core + netty + zookeeper 全家桶。

## 类清单

| 类                                                                                                                                                              | 职责                                                                                                      |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| [TomatoRpcConfiguration](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/TomatoRpcConfiguration.java)                               | 唯一的 `@Configuration`：造 `RpcCoreService`、`RpcStubPostProcessor`、`MonitorController`，监听容器事件   |
| [TomatoRpcProperties](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/data/TomatoRpcProperties.java)                                          | `@ConfigurationProperties(prefix = "tomato-rpc")`                                                         |
| [RpcFactoryBeanDefinitionPostProcessor](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/RpcFactoryBeanDefinitionPostProcessor.java) | `BeanDefinitionRegistryPostProcessor`：扫描所有 bean 定义、为 `@RpcClientStub` 字段注册 `StubFactoryBean` |
| [StubFactoryBean](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/StubFactoryBean.java)                                             | `FactoryBean`：`getObject()` 里调 `rpcCoreService.createStub(...)`                                        |
| [RpcStubPostProcessor](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/RpcStubPostProcessor.java)                                   | `BeanPostProcessor`：注入 stub 字段、注册 `@RpcServerStub` provider                                       |
| `ClientStubMetadata`                                                                                                                                            | 把 `@RpcClientStub` + 字段类型抽成值对象，提供 `uniqueKey()` 和 `equals/hashCode`                         |
| `RpcClientStub` / `RpcServerStub`                                                                                                                               | 两个注解；`RpcServerStub` 被 `@Component` 元注解覆盖                                                      |
| [MonitorController](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/controller/MonitorController.java)                                        | 四个本地 HTTP 接口，路径常量来自 `common` 的 `ApiUrl`                                                     |
| `InvokerMetaVO` / `InvokerStatusVO`                                                                                                                             | 监控接口的返回结构                                                                                        |

## 装配时序

这是整个 Spring 集成里最需要看清的部分，按发生顺序：

```text
1) 解析配置类
   TomatoRpcConfiguration  +  @Import(RpcFactoryBeanDefinitionPostProcessor)

2) BeanDefinitionRegistryPostProcessor（早于任何普通 bean 实例化）
   RpcFactoryBeanDefinitionPostProcessor：
     遍历 registry 里已有的 bean 定义（含 @ComponentScan 扫到的）
     → 反射读字段上的 @RpcClientStub
     → 每个唯一配置注册一个 StubFactoryBean（bean name = uniqueKey）

3) 普通 bean 实例化
   rpcCoreService()：
     microServiceId 为空 → 直接抛 MICROSERVICE_ID_NOT_FOUND
     properties → RpcConfig.Builder（只覆盖非 null 的项）
     group / nameServiceURI 允许 -D 覆盖
     SpiLoader.load(RpcCoreServiceFactory).create(config)  → NettyRpcCoreService

4) BeanPostProcessor 后处理（每个 bean 初始化后）
   RpcStubPostProcessor：
     注入 @RpcClientStub 字段（从 beanFactory 取 FactoryBean 产物）
     发现 @RpcServerStub 类 → rpcCoreService.registerProvider(bean, 接口)

5) ContextRefreshedEvent
   rpcCoreService.init(); rpcCoreService.start();

6) ContextClosedEvent
   rpcCoreService.stop();
```

几个关键点：

- **provider 注册发生在 `init/start` 之前**，只是往内存 map 里放，不涉及网络，所以顺序没问题。
- `init/start/stop` 本身是 CAS 状态机（`BaseLifeCycleComponent`），重复触发会直接 `return`，因此 `ContextRefreshedEvent` 被多次发布也不会炸。
- `StubFactoryBean` 的 `addAutowiredProperty("rpcCoreService")` + `AUTOWIRE_BY_TYPE`，意味着 `rpcCoreService` 会在 BPP 链早期就被创建（Spring 会打一条 `not eligible for getting processed by all BeanPostProcessors` 的 warn），这是刻意换来的「字段注入不用等到依赖注入阶段」。

## 配置项

| 键（`tomato-rpc.` 前缀） | 类型    | 默认             | 说明                              |
| ------------------------ | ------- | ---------------- | --------------------------------- |
| `micro-service-id`       | String  | 无（**必填**）   | 服务唯一标识                      |
| `subscribed-services`    | List    | 空               | 要订阅的下游服务 id               |
| `name-service-uri`       | String  | `RpcConfig` 默认 | ZK 地址，如 `127.0.0.1:2181`      |
| `port`                   | Integer | 默认端口         | RPC 监听端口                      |
| `business-thread`        | Integer | 1                | 服务端业务线程数，≤1 不启用线程池 |
| `stage`                  | String  | `dev`            | 环境，注册路径与订阅路径都用它    |
| `group`                  | String  | `main`           | 分组，实例变量之一                |
| `server-idle-check-ms`   | Long    | 600000           | 服务端读空闲阈值                  |
| `client-keep-alive-ms`   | Long    | 200000           | 客户端心跳间隔                    |
| `enable-circuit`         | boolean | false            | 是否启用熔断                      |
| `circuit-open-rate`      | Integer | —                | 百分比，构建时 `/100.0` 转成小数  |
| `circuit-open-seconds`   | Long    | —                | 打开后多久进入半开                |
| `circuit-window`         | Integer | —                | 采样窗口长度                      |

`stage`、`group`、`name-service-uri` 三项可以在启动时用 JVM 参数覆盖，但**代码路径不同**：

| 配置               | 覆盖位置                                                         |
| ------------------ | ---------------------------------------------------------------- |
| `stage`            | `BaseRpcCoreService#getStage()` 读 `-Dtomato-rpc.service-stage`  |
| `group`            | `TomatoRpcConfiguration` 里读 `-Dtomato-rpc.micro-service-group` |
| `name-service-uri` | 同上，读 `-Dtomato-rpc.name-service-uri`                         |

样例 yaml 里还出现了 `use-gzip: false`，**它没有任何对应字段**，Spring Boot 默认忽略未绑定属性，所以这个开关是无效的；压缩只能用 `@RpcClientStub(compressBody = true)` 逐接口开。

## 两个注解

```java
public @interface RpcClientStub {
    String microServiceId();
    long timeout() default 5000;
    String group() default "";       // 空表示用自身的 group
    boolean compressBody() default false;
}
```

| 注解             | 位置                    | 作用                                                                                                           |
| ---------------- | ----------------------- | -------------------------------------------------------------------------------------------------------------- |
| `@RpcClientStub` | 字段（类型必须是接口）  | 生成动态代理并注入。`microServiceId` + `timeout` + `group` + `compressBody` 共同决定 bean 名                   |
| `@RpcServerStub` | 类（自带 `@Component`） | 把该 bean 注册为 provider；`interfaceTypes()` 用于实现类实现了多个接口时显式指定，不指定则取第一个有接口的父类 |

`ClientStubMetadata.uniqueKey()` 的格式是 `接口全名_microServiceId_timeout_group_compressBody`。这意味着：

- 两个 bean 用**完全一样**的配置注入同一接口 → 共用同一个代理实例。
- 改 `timeout` 会生成新 key，新代理和旧代理并存（旧的多半已经没人引用，但不报错）。
- 字段类型不是接口时 `create` 返回 `Optional.empty()`，注解被静默忽略。

## 本地监控接口

`MonitorController` 直接用 `ApiUrl` 的四个常量，dashboard 也读同一批常量：

| 方法 | 路径                                                    | 说明                                                      |
| ---- | ------------------------------------------------------- | --------------------------------------------------------- |
| GET  | `/api/tomato/invoker/local/status?microServiceId=..`    | 按 `stage → group` 分组的实例列表（含 host/port/weight）  |
| GET  | `/api/tomato/invoker/local/ready`                       | 未就绪返回 503 + `not ready`，可直接当 k8s readinessProbe |
| GET  | `/api/tomato/router/local/list?routerMicroServiceId=..` | 本机生效的表达式列表                                      |
| POST | `/api/tomato/router/local/refresh`                      | dashboard 下发入口，body 为 `RouterRefreshDTO`            |

`refresh` 的两处细节：

```java
List<String> expressionList = request.getRpcRouterDTOList().stream()
    .sorted(Comparator.comparing(RpcRouterDTO::getPriority))
    .map(RpcRouterDTO::getExpr)
    .toList();
microService.get().refreshRouter(request.getOpsGlobalId(), expressionList);
```

- 表达式**先按 `priority` 升序排**再交给路由，所以 dashboard 里的「优先级」就是匹配顺序。
- 这里不做任何表达式校验；非法表达式会在 `refreshRouter` 内部被 catch 掉并打 error 日志，HTTP 仍然返回 200。

## 已知问题

| #   | 问题                                              | 影响                                                          |
| --- | ------------------------------------------------- | ------------------------------------------------------------- |
| 1   | `refresh` 吞异常后仍返回 200                      | 运维以为下发成功，实际旧规则还在（且 `opsGlobalId` 已被推进） |
| 2   | 没有 dashboard → 实例的鉴权                       | 任何能访问 9090 的人都能改路由                                |
| 3   | 监控接口路径写死在业务应用的端口上                | 与业务 HTTP 端口混用，需要额外做暴露策略                      |
| 4   | `use-gzip` 这类无效配置静默忽略                   | yaml 看起来生效了，其实没有任何作用                           |
| 5   | `RpcCoreService` 没有 `@ConditionalOnMissingBean` | 想提供自定义 `RpcCoreService` bean 需要自己排除自动配置       |
| 6   | 同一接口多配置会产生多个代理 bean                 | 没有「按接口收敛」的逻辑，排查时容易看错是哪一个在调用        |
| 7   | 只支持 Spring 容器内的 bean 使用注解              | 手工 `new` 出来的对象不会被 `BeanPostProcessor` 处理          |

## 测试

`TomatoRpcConfigurationTest` 用 `@SpringBootTest` + `@ComponentScan(config.component, config.test)` + `@ActiveProfiles("test")` 真跑一遍自动装配，`@BeforeClass` 里起 `TestingServer(3689)` 当注册中心：

| 用例                          | 断言                                                                                                                                                                    |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `configurationTest`           | `TomatoRpcProperties` 全部字段绑定正确；`TestApi` 的 bean 数 = 1 个 server stub + 1 个 client stub，`TestTimeoutApi` = 1 + 2（两个 bean 用不同 timeout 注入了同一接口） |
| `beanRegisterTest`            | `testClientBean.getTestApi().echo("echo")` 真实走一遍 ZK 订阅 + Netty 调用并返回 `echo`                                                                                 |
| `annotationTimeoutConfigTest` | 注入时配了极小 timeout 的接口调用会抛 `RPC_INVOCATION_TIMEOUT`                                                                                                          |

注意 `@AfterClass` 里先 `rpcCoreService.stop()` 再关 `TestingServer`——顺序反了会在关闭时刷一堆 ZK 连接异常。

详见 [测试](./../13-testing.md) 与 [Spring 集成](./../10-spring.md)。

## 阅读顺序

1. [TomatoRpcConfiguration](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/TomatoRpcConfiguration.java)：先看入口和生命周期钩子。
2. [RpcFactoryBeanDefinitionPostProcessor](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/RpcFactoryBeanDefinitionPostProcessor.java) → [StubFactoryBean](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/StubFactoryBean.java)：代理是怎么变出来的。
3. [RpcStubPostProcessor](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/component/RpcStubPostProcessor.java)：注入与注册。
4. [MonitorController](../../tomato-rpc-config/src/main/java/org/tomato/study/rpc/config/controller/MonitorController.java)：本机和 dashboard 的接口约定。

配套章节：[Spring 集成](./../10-spring.md)、[监控与运营](./../11-dashboard.md)。
