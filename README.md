# Tomato-RPC

为了巩固微服务与 RPC 基础原理而写的 RPC 框架：基于 Netty 实现网络通信与自定义二进制协议，基于 ZooKeeper 实现服务注册/订阅，自研表达式引擎做灰度路由，并附了一套最小可用的熔断、负载均衡与运维接口。设计上参考了 Dubbo 与 Feign 的一些思路。

> 这是一个**学习型项目**：功能闭环完整、代码量可控、每处取舍都能在源码里找到；不追求生产级健壮性，文档中会把已知缺陷一并写出来。

## 核心特性

| 能力           | 实现要点                                                                                                     |
| -------------- | ------------------------------------------------------------------------------------------------------------ |
| 自定义协议     | 19 字节帧头（magic + length + version + 扩展头长度 + 命令类型 + 序列化类型 + 消息 id），支持扩展头 `k:v&k:v` |
| 网络通信       | Netty 4.1，服务端 boss/worker/business 三组线程，客户端长连接 + 心跳 + 时间轮超时                            |
| 服务注册与发现 | ZooKeeper 临时节点 + 一次性 watcher + 单线程串行刷新                                                         |
| 灰度路由       | 自研表达式引擎（词法 + 递归下降 + AST），规则形如 `USER_ID % 10 == 1 -> group == "gray"`                     |
| 负载均衡       | Nginx 同款平滑加权轮询 + 随机，支持按权重调整流量                                                            |
| 熔断           | 三态机 + BitSet/FenwickTree 滑动窗口错误率统计                                                               |
| 可扩展         | 自定义 SPI（`META-INF/tomato/接口全名`）+ 两处 JDK SPI，可替换传输层、注册中心、序列化等                     |
| Spring 集成    | starter + `@RpcClientStub` / `@RpcServerStub`，注解即用                                                      |
| 可观测         | 本机 REST 接口（就绪、实例、路由）+ 独立的 dashboard 应用做规则下发                                          |
| 分布式上下文   | `InvocationContext` + 扩展头透传 traceId，自动写入 MDC                                                       |

技术栈：JDK 17、Spring Boot 3.2.12、Netty 4.1.97、ZooKeeper 3.5.9 / Curator 5.9、Protostuff 1.8、Maven 多模块。

## 模块一览

| 模块                                                 | 说明                                           |
| ---------------------------------------------------- | ---------------------------------------------- |
| `tomato-rpc-common`                                  | Logger、NetworkUtil、FenwickTree、监控路径常量 |
| `tomato-rpc-expression`                              | 表达式引擎（零框架依赖）                       |
| `tomato-rpc-core`                                    | 全部抽象、数据模型、调用链、路由、熔断、SPI    |
| `tomato-rpc-netty`                                   | **网络通信实现**：编码解码、连接管理、线程模型 |
| `tomato-rpc-registry-zookeeper`                      | ZooKeeper 注册中心实现                         |
| `tomato-rpc-config`                                  | Spring 装配、注解、本机监控接口                |
| `tomato-rpc-spring-boot-starter`                     | 聚合依赖（只有 pom）                           |
| `tomato-rpc-dashboard`                               | 独立运维应用：查实例、改路由、推送刷新         |
| `tomato-rpc-log4j2-config`                           | 共享日志配置                                   |
| `tomato-rpc-sample-*` / `tomato-rpc-spring-sample-*` | 5 个可运行样例                                 |
| `tomato-rpc-test`                                    | 全部测试                                       |

## 文档目录

### 入门

- [01 项目总览](./docs/01-overview.md)
- [02 快速开始](./docs/02-quick-start.md)
- [03 架构分层](./docs/03-architecture.md)

### 网络通信（重点）

- [04 应用层协议](./docs/04-protocol.md) —— 帧结构、字段语义、扩展头、编解码
- [05 调用链路](./docs/05-invocation.md) —— 从动态代理到字节、超时与响应匹配
- [06 传输层](./docs/06-transport.md) —— 线程模型、Pipeline、连接与心跳、背压

### 服务治理

- [07 注册中心](./docs/07-registry-zookeeper.md)
- [08 服务治理](./docs/08-governance.md) —— 路由、灰度、负载均衡、熔断
- [09 SPI 与生命周期](./docs/09-spi-lifecycle.md)

### 平台与运维

- [10 Spring 集成](./docs/10-spring.md)
- [11 监控与运营](./docs/11-dashboard.md)
- [12 表达式引擎](./docs/12-expression.md)
- [13 测试](./docs/13-testing.md)
- [14 部署](./docs/14-deployment.md)

### 模块实现说明

- [tomato-rpc-netty](./docs/modules/tomato-rpc-netty.md)（最详细）
- [tomato-rpc-core](./docs/modules/tomato-rpc-core.md)
- [tomato-rpc-registry-zookeeper](./docs/modules/tomato-rpc-registry-zookeeper.md)
- [tomato-rpc-config](./docs/modules/tomato-rpc-config.md)
- [tomato-rpc-expression](./docs/modules/tomato-rpc-expression.md)
- [tomato-rpc-dashboard](./docs/modules/tomato-rpc-dashboard.md)
- [tomato-rpc-common](./docs/modules/tomato-rpc-common.md)
- [tomato-rpc-spring-boot-starter](./docs/modules/tomato-rpc-spring-boot-starter.md)
- [样例与测试](./docs/modules/samples-and-test.md)

## 三十秒上手

```bash
# 1. 起一个 ZooKeeper（默认 127.0.0.1:2181）
docker run -d -p 2181:2181 zookeeper:3.5.9
# 2. 构建（assembly 已把启动类写进 manifest）
mvn -DskipTests package
# 3. 起样例服务端，再另开一个终端起客户端
java -jar tomato-rpc-sample-server/target/tomato-rpc-sample-server-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar tomato-rpc-sample-client/target/tomato-rpc-sample-client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Spring 项目只需引 `tomato-rpc-spring-boot-starter`，配置 `tomato-rpc.micro-service-id` 与 `name-service-uri`，给实现类加 `@RpcServerStub`、给字段加 `@RpcClientStub` 即可。详见 [快速开始](./docs/02-quick-start.md)。

## 目录结构

```text
tomato-rpc/
├── tomato-rpc-*/          # 10 个框架/应用模块
├── k8s-yaml/              # 部署样例
├── uml/                   # 类图与时序图
├── docs/                  # 全部文档（本 README 是它的目录）
└── pom.xml
```

## 许可证

[Apache License 2.0](./LICENSE)
