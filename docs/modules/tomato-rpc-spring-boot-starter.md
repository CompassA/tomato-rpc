# tomato-rpc-spring-boot-starter

> 一个**只有 pom、没有一行代码**的聚合模块。它的存在只是为了给业务方一个「引一个坐标就够」的入口。

## 内容

```xml
<artifactId>tomato-rpc-spring-boot-starter</artifactId>
<dependencies>
    <dependency>
        <groupId>org.tomato.study.rpc</groupId>
        <artifactId>tomato-rpc-config</artifactId>
    </dependency>
</dependencies>
```

没有 `src/main`，没有 `spring.factories`，没有自动配置类——**真正的自动配置在 `tomato-rpc-config` 里**。

## 依赖传递链

```text
tomato-rpc-spring-boot-starter
└── tomato-rpc-config
    ├── tomato-rpc-netty
    │   ├── tomato-rpc-core
    │   │   ├── tomato-rpc-common
    │   │   └── tomato-rpc-expression
    │   └── tomato-rpc-registry-zookeeper
    │       └── tomato-rpc-core（已提升，Maven 去重）
    └── tomato-rpc-core（已提升）
```

所以业务方写一个依赖就拿到了：Spring 装配、Netty 传输、ZK 注册中心、表达式引擎。这也是 `tomato-rpc-spring-sample-*` 的 pom 只有一条依赖的原因。

## 怎么用

```xml
<dependency>
    <groupId>org.tomato.study.rpc</groupId>
    <artifactId>tomato-rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

```yaml
tomato-rpc:
  micro-service-id: "demo-rpc-service"
  name-service-uri: "127.0.0.1:2181"
  port: 4567
  subscribed-services:
    - "demo-downstream-rpc-service"
```

```java
@RpcServerStub
public class EchoServiceImpl implements EchoService { ... }

@RpcClientStub(microServiceId = "demo-rpc-service", timeout = 1000)
private EchoService echoService;
```

然后什么都不用做：`TomatoRpcConfiguration` 会在容器刷新完成时 `init + start`，关闭时 `stop`。详见 [Spring 集成](./../10-spring.md) 与 [tomato-rpc-config](./tomato-rpc-config.md)。

## 为什么单独拆一个模块

| 原因       | 说明                                                                                   |
| ---------- | -------------------------------------------------------------------------------------- |
| 命名惯例   | `xxx-spring-boot-starter` 是给使用者的坐标，`xxx-config` 是实现模块，两者职责分开      |
| 未来可扩展 | 如果以后加 `tomato-rpc-spring-boot-autoconfigure`，只改这个 pom 的依赖，业务方坐标不变 |
| 版本收敛   | 版本由父 pom 的 `dependencyManagement` 统一管理，示例工程不用写版本号                  |

本仓库目前**没有**把自动配置拆成 `autoconfigure` 模块，所以直接依赖 `tomato-rpc-config` 效果完全一样——starter 只是一个语义化的空壳。

## 已知问题

| #   | 问题                                                       | 影响                                                                                               |
| --- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| 1   | starter 不含任何代码                                       | 想通过 starter 覆盖自动配置，需要自己去 `exclude` `TomatoRpcConfiguration`                         |
| 2   | 传递依赖了 netty + zookeeper + protostuff                  | 引进来就固定了这套实现，想换传输层要手工排除再引别的实现                                           |
| 3   | 没有 `@ConditionalOnClass` / `@ConditionalOnProperty` 保护 | 只要 classpath 上有 config，就一定尝试创建 `RpcCoreService`（`micro-service-id` 没配会直接抛异常） |
| 4   | 版本 `1.0-SNAPSHOT`                                        | 没有发布到远程仓库，本地使用前要先 `mvn install`                                                   |

## 阅读顺序

这个模块没有代码可读。想理解「引进来之后发生了什么」，直接看：

1. [tomato-rpc-config](./tomato-rpc-config.md)：装配时序、配置项、两个注解。
2. [tomato-rpc-netty](./tomato-rpc-netty.md)：SPI 默认实现是怎么被选中的。
3. [部署](./../14-deployment.md)：构建顺序与进程拓扑。
