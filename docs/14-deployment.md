# 部署与运维

仓库里能跑起来的进程只有三类，部署时先把它们的职责和端口区分清楚，后面所有问题都好排查：

| 进程               | 是否必须 | 端口                                | 说明                                             |
| ------------------ | -------- | ----------------------------------- | ------------------------------------------------ |
| ZooKeeper          | 必须     | 2181                                | 注册中心。没它就退化成直连模式                   |
| 业务应用（server） | 至少一个 | RPC 端口 4567 / HTTP 端口 9090      | 暴露接口，同时可订阅下游                         |
| 业务应用（client） | 可选     | RPC 端口 4567（也要占） / HTTP 9090 | 每次 `start` 都会起 Netty server，即使只做消费者 |
| dashboard          | 可选     | 12222                               | 读写 ZK、下发路由规则；挂了不影响业务流量        |

一个容易忽略的点：**客户端进程也会监听 RPC 端口**。`RpcCoreService#start` 里 server 一定会启（哪怕没有 provider），所以同一台机器上同时起 sample-server 和 sample-client 会端口冲突——必须改端口或分容器。

## 端口与地址清单

| 位置            | 值                        | 出处                                                                                                            |
| --------------- | ------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 注册中心        | `127.0.0.1:2181`          | 各 `application.yaml` 的 `name-service-uri`                                                                     |
| 样例服务端 RPC  | `4567`                    | [spring-sample-server application.yaml](../tomato-rpc-spring-sample-server/src/main/resources/application.yaml) |
| 样例服务端 HTTP | `9090`                    | 同上 `server.port`（dashboard 推送目标端口）                                                                    |
| 样例客户端 RPC  | `4567`                    | [spring-sample-client application.yaml](../tomato-rpc-spring-sample-client/src/main/resources/application.yaml) |
| 样例客户端 HTTP | `9090`                    | 同上                                                                                                            |
| 下游 dev 环境   | RPC `4568` / HTTP `13334` | `application-downstream-dev.yaml`、`application-default.yaml`                                                   |
| dashboard       | `12222`                   | [dashboard application.yaml](../tomato-rpc-dashboard/src/main/resources/application.yaml)                       |
| ZK 命名空间     | `/tomato`                 | `CuratorClient` 的 `namespace("tomato")`                                                                        |

## 构建

```bash
# 全部模块装进本地仓库（跳过测试）
mvn clean install -DskipTests

# 打可执行 fat jar（三个 Spring 应用都配了 spring-boot-maven-plugin + mainClass）
mvn -pl tomato-rpc-spring-sample-server -am package spring-boot:repackage -DskipTests
mvn -pl tomato-rpc-spring-sample-client -am package spring-boot:repackage -DskipTests
mvn -pl tomato-rpc-dashboard            -am package spring-boot:repackage -DskipTests
```

产物：`target/tomato-rpc-spring-sample-server-1.0-SNAPSHOT.jar` 等。非 Spring 的 `tomato-rpc-sample-server` / `tomato-rpc-sample-client` 是普通 jar，用 `java -cp` 或 IDE 直接跑 `DemoServerApplication` / `DemoClientApplication`。

## Docker

根目录 5 个 Dockerfile，**构建前先看清每个文件实际打的是哪个模块**：

| 文件                              | 实际 COPY 的 jar                               | 能否直接用                                                    |
| --------------------------------- | ---------------------------------------------- | ------------------------------------------------------------- |
| `sample_server.Dockerfile`        | `tomato-rpc-spring-sample-server/target/*.jar` | 文件头标了 `# deprecated`；JDK 11（与 JDK 17 构建产物不兼容） |
| `sample_client.Dockerfile`        | `tomato-rpc-spring-sample-client/target/*.jar` | 同上，已废弃                                                  |
| `spring_client_sample.Dockerfile` | `tomato-rpc-spring-sample-server/...`          | **名字与内容相反**（打的是 server），`EXPOSE 9090`            |
| `spring_server_sample.Dockerfile` | `tomato-rpc-spring-sample-client/...`          | **名字与内容相反**（打的是 client）                           |
| `spring_dashboard.Dockerfile`     | `tomato-rpc-dashboard/...`                     | 正常，`EXPOSE 12222`                                          |

以 dashboard 为例的完整用法：

```bash
mvn clean install -DskipTests && \
mvn -pl tomato-rpc-dashboard -am package spring-boot:repackage -DskipTests

docker build -t compassa/tomato-rpc-dashboard:1.0.3 -f ./spring_dashboard.Dockerfile .

docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181" \
           -d compassa/tomato-rpc-dashboard:1.0.3
```

三个新版 Dockerfile 的入口都是 `java -jar $JAVA_OPTIONS <jar>`——`JAVA_OPTIONS` 是**唯一的配置注入点**，`-D` 参数直接透传给 JVM。旧的两个是 `ENTRYPOINT java -jar $JAVA_OPTIONS *.jar` 形式（依赖通配符，只适用单 jar 目录）。

## Kubernetes

`k8s-yaml/` 只有两个 Deployment，ZK 的 StatefulSet 定义写在旧 README 里（已迁移到本文末尾的附录思路）。整体拓扑：

```text
namespace: tomato
├── Service(zookeeper, headless) + StatefulSet(zookeeper-set, 1 replica)
│   └── DNS: zookeeper-set-0.zookeeper.tomato.svc.cluster.local:2181
├── Deployment(rpc-sample-deployment, 3 replicas)   → 服务端
└── Deployment(sample-client-job, 1 replica)        → 客户端
```

两个 Deployment 的共同点：

```yaml
env:
  - name: "JAVA_OPTIONS"
    value: "-Dtomato-rpc.name-service-uri=zookeeper-set-0.zookeeper.tomato.svc.cluster.local:2181"
```

JVM 参数覆盖 `name-service-uri` 是这里唯一的配置来源——镜像里没有改 yaml，靠 `TomatoRpcConfiguration` 用 `System.getProperty` 兜底。

### 三个 yaml 级别的坑

| #   | 问题                                    | 说明                                                                                                                           |
| --- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| 1   | `containerPort: 1535`                   | 实际 RPC 端口是 `4567`（`DemoServerApplication` 里硬编码），1535 是历史遗留值。因为没用 Service 转发，不影响连通，但会误导排障 |
| 2   | 没有 `readinessProbe` / `livenessProbe` | 框架自带 `/api/tomato/invoker/local/ready`（就绪返 200，否则 503），但 yaml 没配，滚动更新期间流量会打到未就绪实例             |
| 3   | 客户端也用 Deployment                   | 客户端不是常驻任务却按 Deployment 部署，重启靠 `replicas: 1` 兜底，日志要 `kubectl logs -f` 才能看到                           |

### 验证部署是否成功

```bash
kubectl get pods -n tomato -l app=rpc-sample-server-pod

# 在 ZK 里确认注册成功（路径每段都被 URLEncoder 编码过，看到 %3A 是正常的）
kubectl exec -it zookeeper-set-0 -n tomato -- bash
zkCli.sh
[zk] ls /tomato/demo-rpc-service/dev/providers
[tomato%3A%2F%2F10.42.219.80%3A4567%2F%3Fmicro-service-id%3Ddemo-rpc-service%26stage%3Ddev%26group%3Dmain]

# 业务侧看到 echo 响应即链路打通
kubectl logs -f <sample-client-pod> -n tomato
```

## 配置注入的四种方式

| 方式                     | 例子                                                                                                                                                                    | 适用                                            |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| yaml                     | `tomato-rpc.stage: prod`                                                                                                                                                | 常规部署                                        |
| JVM 参数                 | `-Dtomato-rpc.name-service-uri=...`                                                                                                                                     | 容器/环境差异（`group`、`nameServiceUri` 支持） |
| JVM 参数（框架专用 key） | `-Dtomato-rpc.service-stage=gray`、`-Dtomato-rpc.service-group=gray`、`-Dtomato-rpc.subscribed-services-group=svcA:gray&svcB:main`、`-Dtomato-rpc.spi=<接口全名>:<key>` | 环境隔离、灰度、替换 SPI 实现                   |
| mock profile             | `-Dspring.profiles.active=prod` / `gray`                                                                                                                                | 样例的多环境演示                                |

优先级（从高到低）：JVM `-D`（框架 key）> yaml / Spring 配置。`stage` 的覆盖发生在 `BaseRpcCoreService#getStage`，`group` 与 `nameServiceUri` 发生在 `TomatoRpcConfiguration`，`subscribed-services-group` 在 `BaseMicroServiceSpace#fetchTargetGroup` 里读取——也就是说这三个参数**走的是三条不同的代码路径**，排障时要分别看。

样例的多环境演示：

```bash
# 服务端：dev（默认 4567/9090）、prod/gray、downstream-dev/gray/prod
java -jar tomato-rpc-spring-sample-server.jar --spring.profiles.active=gray

# 客户端：default / prod / gray
java -jar tomato-rpc-spring-sample-client.jar --spring.profiles.active=gray
```

`application-gray.yaml` 只覆盖 `stage: prod` + `group: gray`，端口沿用 `application.yaml`，所以**同机只能起一个实例**。

## 优雅关闭

`RpcCoreService#stop` 的调用链：`BaseRpcCoreService#doStop` → 关闭 invoker（最多等 60s）→ 关闭 Netty server → 关闭注册中心连接。ZK 上的 `providers` 是 `EPHEMERAL` 节点，**session 断开即下线**，所以只要进程正常退出（或无响应超过 session timeout），实例就会从注册表消失，客户端靠 watcher 收到变更并移除 invoker。

Spring 应用在 `ContextClosedEvent` 时自动调用 `stop()`。容器场景要注意：

| 事实                                             | 后果                                                                              |
| ------------------------------------------------ | --------------------------------------------------------------------------------- |
| 没有 preStop hook / 没有 readinessProbe 先行摘流 | 滚动更新时会有几秒请求打到正在关闭的实例                                          |
| 关闭流程最长 60s                                 | `terminationGracePeriodSeconds` 默认 30s，长调用会被 SIGKILL，连接靠 TCP/RST 断开 |
| ZK 会话超时 60s（`CuratorClient`）               | 进程崩溃后节点最长 60s 才消失                                                     |

## 日志

框架的日志门面是 [Logger](../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/utils/Logger.java)，两个 logger 名：

| logger 名            | 内容                                                                |
| -------------------- | ------------------------------------------------------------------- |
| `TOMATO-RPC-DEFAULT` | 生命周期、注册订阅、路由刷新、异常堆栈                              |
| `TOMATO-RPC-DIGEST`  | 摘要行，形如 `\|refresh-router\|Y/N\|耗时\|opsId\|`，便于 grep/统计 |

`tomato-rpc-log4j2-config` 模块提供了一份可直接引用的 `log4j2.xml`：

```text
logs/tomato-rpc/default.log        普通日志，512MB 滚动，保留 5 个
logs/tomato-rpc/common_error.log   错误日志
```

它在 `TOMATO-RPC-DEFAULT`/`DIGEST` 之外还配了 `TERMINAL_RPC_CONTEXT_PATTERN`——**样例模块的 log4j2 用的是带 `%X{TRACE_ID}` 的格式**，因为 `BaseStubInvoker` 会把 traceId 塞进 MDC。要在自己的应用里看到 traceId，就得用这个 pattern 或者自己加 MDC 字段。

## 发布前自检

```bash
# 1. 就绪状态（200 "" 才算好）
curl -i http://<host>:9090/api/tomato/invoker/local/ready

# 2. 订阅到的实例是否和预期一致（按 stage → group 分组）
curl http://<host>:9090/api/tomato/invoker/local/status?microServiceId=demo-rpc-service

# 3. 本机生效的路由规则
curl http://<host>:9090/api/tomato/router/local/list?routerMicroServiceId=demo-rpc-service

# 4. 注册中心里的实例（注意 URL 编码）
zkCli.sh -server <zk>:2181
ls /tomato/demo-rpc-service/dev/providers
```

| 检查项                                         | 常见故障                                                                       |
| ---------------------------------------------- | ------------------------------------------------------------------------------ |
| `/ready` 一直 503                              | provider 未注册、订阅未完成、ZK 连不上                                         |
| `status` 为空                                  | `stage` 不匹配（客户端订阅的 stage 与服务端注册的不同）                        |
| 能查到实例但调用报 `STUB_INVOKER_SEARCH_ERROR` | 路由规则把所有候选筛掉了（见 [表达式引擎](./12-expression.md) 的「错误路径」） |
| 调用报 `InvocationTimeout`                     | 服务端业务线程池被打满、或 `business-thread: 1` 时被慢方法串行阻塞             |
| dashboard 下发成功但规则没变                   | 推送端口不是 9090、实例没注册 provider（收不到推送）、opsId 未递增             |

## 已知限制

| #   | 限制                                                                                     |
| --- | ---------------------------------------------------------------------------------------- |
| 1   | 没有 VM/Helm/CI 交付物，`k8s-yaml/` 只是两个 Deployment 示例                             |
| 2   | 两个 `sample_*.Dockerfile` 已废弃且基于 JDK 11，与 JDK 17 产物不兼容                     |
| 3   | 两个 `spring_*_sample.Dockerfile` 文件名与内容互换                                       |
| 4   | 客户端进程强制占用 RPC 端口，同机多实例必须改端口                                        |
| 5   | dashboard 推送目标端口硬编码 9090                                                        |
| 6   | 容器内注册的 host 取自 `NetworkUtil` 的本机 IP，NAT/多网卡环境下需要确认上报的是可达地址 |
| 7   | 无 metrics（Prometheus）暴露，只有 HTTP JSON 接口                                        |
| 8   | 无健康检查探针配置，需自行补 `readinessProbe` 指向 `/api/tomato/invoker/local/ready`     |

相关章节：[快速开始](./02-quick-start.md)、[注册中心](./07-registry-zookeeper.md)、[监控与运营](./11-dashboard.md)、[测试](./13-testing.md)。
