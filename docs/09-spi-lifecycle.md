# SPI 与生命周期

## 两套扩展机制

框架里同时存在两种插件机制，用途不同：

| 机制       | 配置文件                           | 装载器          | 用于                                                                                                        |
| ---------- | ---------------------------------- | --------------- | ----------------------------------------------------------------------------------------------------------- |
| 自定义 SPI | `META-INF/tomato/{接口全限定名}`   | `SpiLoader`     | 框架自身的「可替换组件」：核心服务工厂、invoker 工厂、stub 工厂、负载均衡、熔断工厂、序列化器、注册中心工厂 |
| JDK SPI    | `META-INF/services/{接口全限定名}` | `ServiceLoader` | 需要「一次加载多个实现」的场景：序列化器索引表、服务端 `ServerHandler` 列表                                 |

为什么不统一用 JDK SPI？因为框架需要 **默认实现 + 按 key 选择 + 单例 + 依赖注入**，JDK `ServiceLoader` 只能给出一串实现，选谁、什么时候 new、依赖怎么注入都得自己写；而 `Serializer` / `ServerHandler` 恰好是「全都要」的语义，用 JDK SPI 更直接。

## 扩展点清单

| 接口                    | `@SpiInterface` 默认 key | 配置文件所在模块                           | 现有实现                                 |
| ----------------------- | ------------------------ | ------------------------------------------ | ---------------------------------------- |
| `RpcCoreServiceFactory` | `netty`                  | core、config、sample-server、sample-client | `NettyRpcCoreServiceFactory`             |
| `RpcInvokerFactory`     | `netty`                  | netty                                      | `NettyRpcInvokerFactory`                 |
| `StubFactory`           | `jdk`                    | core                                       | `JdkStubFactory`                         |
| `LoadBalance`           | `weight-round-robin`     | core                                       | `RoundRobinLoadBalance`                  |
| `CircuitBreakerFactory` | `default`                | core                                       | `DefaultCircuitBreakerFactory`           |
| `Serializer`            | `json`                   | core                                       | `JsonSerializer`、`ProtostuffSerializer` |
| `NameServerFactory`     | `zookeeper-factory`      | registry-zookeeper                         | `ZookeeperNameServerFactory`             |

> 同一个接口的配置文件可以出现在**多个 jar** 里，`loadSpiConfigFile` 会用 `classLoader.getResources(path)` 枚举所有同名资源并合并。因此 `tomato-rpc-config` 和 sample 模块各自放了一份 `RpcCoreServiceFactory` 配置，保证「只引 netty、不引 config」的场景也能加载到实现。

除 SPI 外，还有三处「隐式扩展点」，靠注解或约定工作：

| 扩展点               | 机制                                                               | 位置                                    |
| -------------------- | ------------------------------------------------------------------ | --------------------------------------- |
| `ServerHandler`      | JDK SPI，按 `getType()` 建 map                                     | `META-INF/services/...ServerHandler`    |
| `CommandInterceptor` | 硬编码数组 `new CommandInterceptor[]{ new CompressInterceptor() }` | `DispatcherHandler` / `ResponseHandler` |
| `LifeCycleListener`  | 接口存在但**没有触发点**                                           | `BaseLifeCycleComponent`                |

## 自定义 SPI 的工作方式

### 文件格式

```text
# META-INF/tomato/org.tomato.study.rpc.core.serializer.Serializer
json : org.tomato.study.rpc.core.serializer.JsonSerializer
protostuff : org.tomato.study.rpc.core.serializer.ProtostuffSerializer
```

- 一行一个 `key : 实现类全限定名`，`:` 前后空格会被 trim。
- 没有 `:` 或 `:` 在首位的行被跳过（不报错）。
- 实现类必须能通过**无参构造**创建。
- 类加载时会 `Class.forName(implClassName, true, classLoader)` 并校验 `spiInterface.isAssignableFrom(clazz)`，不匹配直接忽略。

### 加载流程

```java
public T load() {
    String priorityKey = JVM_PRIORITY_CONFIG.get(spiInterface.getCanonicalName());
    if (StringUtils.isNotBlank(priorityKey)) {
        T component = load(priorityKey);
        if (component != null) return component;          // JVM 指定的 key 不存在时静默回退
    }
    return load(defaultKey);                              // 注解里的默认 key
}
```

三个层次，优先级从高到低：

1. **JVM 参数**：`-Dtomato-rpc.spi={接口全限定名}:{key}&{接口2}:{key2}`
2. **`@SpiInterface.value()`**：接口上声明的默认 key
3. 都没有 → `load(key)` 返回 `null`，调用方通常直接 NPE（不会兜底）

例：临时把序列化换成 protostuff（两端都要改，否则对端按 `serializeType` 找不到序列化器）：

```bash
-Dtomato-rpc.spi=org.tomato.study.rpc.core.serializer.Serializer:protostuff
```

`SpiLoader.resetPriorityMap()` 可以在运行时重新读取 JVM 参数，供测试使用。

### 单例与依赖注入

`@SpiInterface.singleton()` 默认 `true`：

```java
synchronized (paramName.intern()) {
    T component = singletonMap.get(paramName);
    if (component == null) {
        // 先放进 map，再注入依赖 —— 防止 A 依赖 B、B 依赖 A 时无限递归
        component = singletonMap.computeIfAbsent(paramName, key -> createSpiInstance(spiImplClass));
        injectSpiComponents(spiImplClass, component);
    }
    return component;
}
```

依赖注入是「setter 约定」：遍历实现类的**本类声明方法**，凡是 `public void setXxx(SomeSpiInterface)` 且参数类型是带 `@SpiInterface` 的接口，就注入 `SpiLoader.getLoader(SomeSpiInterface).load()`。

| 特性                        | 说明                                                                                                  |
| --------------------------- | ----------------------------------------------------------------------------------------------------- |
| 只扫 `getDeclaredMethods()` | 父类的 setter 不会被注入                                                                              |
| 只注入接口类型              | 参数是具体类则跳过                                                                                    |
| 循环依赖                    | 先入 map 再注入，A↔B 时能返回「尚未注入完成」的实例（可能拿到半成品，需自己保证 setter 调用顺序安全） |
| `singleton=false`           | 每次 `load` 都 new，不做注入缓存                                                                      |
| 线程安全                    | 以 `paramName.intern()` 为锁，锁粒度是 key 字符串                                                     |

## JDK SPI 的两处使用

### 序列化器索引表

```java
// SerializerHolder
static {
    ServiceLoader.load(Serializer.class)
        .forEach(serializer -> SERIALIZER_MAP.put(serializer.serializerIndex(), serializer));
}
```

索引是**写死的常量**（`ProtostuffSerializer` = 0、`JsonSerializer` = 1），必须与对端一致。`META-INF/services/...Serializer` 里两个实现都要登记，否则收到对方发的帧会 `getSerializer` 返回 null。

### 服务端 Handler

```java
// DispatcherHandler 构造
ServiceLoader<ServerHandler> serverHandlers = ServiceLoader.load(ServerHandler.class);
for (ServerHandler h : serverHandlers) {
    h.setProviderRegistry(providerRegistry);       // 注意：这里是普通 setter，不是 SPI 注入
    serverHandlerMap.put(h.getType(), h);
}
```

| 实现                      | 处理的 `CommandType` |
| ------------------------- | -------------------- |
| `RpcRequestHandler`       | `RPC_REQUEST`        |
| `KeepAliveRequestHandler` | `KEEP_ALIVE_REQUEST` |

**扩展方式**：写一个实现类 + 在 `META-INF/services/org.tomato.study.rpc.core.ServerHandler` 里加一行全限定名，无需改任何框架代码。这也是框架里最"干净"的扩展点。

## 生命周期

### 状态机

```text
CREATED ──init()──▶ INIT ──▶ INIT_FINISHED ──start()──▶ START ──▶ START_FINISHED ──stop()──▶ STOP ──▶ STOP_FINISHED
```

`BaseLifeCycleComponent` 的三个方法都是「CAS 抢不到就 return」：

| 调用                     | 结果                                                        |
| ------------------------ | ----------------------------------------------------------- |
| 重复 `init()`            | 第二次 CAS 失败，静默返回                                   |
| 未 `init()` 就 `start()` | CAS(`INIT_FINISHED`→`START`) 失败，静默返回（**不会启动**） |
| 未 `start()` 就 `stop()` | 同上，静默返回                                              |
| 中间态推进失败           | 抛 `RPC_COMPONENT_LIFE_CYCLE_INVALID_STATE`                 |

「静默返回」意味着**忘记调 `init()` 会导致服务静默不启动**，排查时先看日志里有没有 `netty rpc core service started`。

### 各组件顺序

`RpcCoreService`（门面）：

| 阶段    | 顺序                                                                                                         |
| ------- | ------------------------------------------------------------------------------------------------------------ |
| `init`  | `rpcServer.init()` → `nameServer.init()`                                                                     |
| `start` | `rpcServer.start()` → `nameServer.start()` → `registerService(自身元数据)` → `subscribe(所有订阅服务)`       |
| `stop`  | `unsubscribe` → `unregisterService` → 逐个 `microService.close()` → `rpcServer.stop()` → `nameServer.stop()` |

被它驱动的组件：

| 组件                  | init                                                          | start                                                                 | stop                                                |
| --------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------- | --------------------------------------------------- |
| `NettyRpcServer`      | 建线程池 + `ServerBootstrap`                                  | `bind(port)`                                                          | `shutdownGracefully` + 业务池 `shutdown`            |
| `ZookeeperNameServer` | `BaseNameServer.doInit`（建刷新线程）+ 建 `ZookeeperRegistry` | `BaseNameServer.doStart`（启动刷新线程）+ `registry.start()`（连 ZK） | 中断刷新线程 + 关 Curator                           |
| `NettyRpcClient`      | 建 `EventLoopGroup` + `Bootstrap`                             | 空实现                                                                | 关 channel + `shutdownGracefully`                   |
| `NettyRpcInvoker`     | 构造即建 client                                               | —                                                                     | `client.stop()` + `timer.stop()`（最多等 60s 排空） |

> Spring 场景下这套顺序由 `TomatoRpcConfiguration` 的事件监听触发，见 [Spring 集成](./10-spring.md)。

### 构造函数里的副作用

`BaseRpcCoreService` 的构造器并不只是赋值，它完成了大量装配：

```java
providerRegistry = new DefalultProviderRegistry();
invokerFactory   = SpiLoader.getLoader(RpcInvokerFactory.class).load();
stubFactory      = SpiLoader.getLoader(StubFactory.class).load();
loadBalance      = SpiLoader.getLoader(LoadBalance.class).load();
nameServer       = SpiLoader.getLoader(NameServerFactory.class).load().createNameService(config);
rpcServer        = createRpcServer(rpcServerConfig, providerRegistry);
microServices    = createMicroServiceSpace(rpcConfig);      // 每个订阅服务一个 MicroServiceSpace
```

这意味着：**SPI 配置错误、注册中心地址非法、端口被占用等问题，在 `new NettyRpcCoreService(config)` 时就会暴露**，而不是等到 `start()`。`NettyRpcClient` 同理——构造时 `init(); start();`，异常只打日志不抛出，因此「连不上」通常会推迟到第一次调用才表现为 `RPC_CONNECTION_TIMEOUT`。

## 新增一个扩展的完整步骤

以「换一个最少连接数负载均衡」为例：

1. 实现接口：

   ```java
   public class LeastConnectionLoadBalance extends BaseLoadBalance {
       @Override
       protected RpcInvoker doSelect(Invocation invocation, List<RpcInvoker> invokers) { ... }
   }
   ```

2. 在自己的模块里建配置文件 `src/main/resources/META-INF/tomato/org.tomato.study.rpc.core.loadbalance.LoadBalance`：

   ```text
   least-connection : com.example.LeastConnectionLoadBalance
   ```

3. 二选一生效：
   - 改接口注解默认值（要动 core，不推荐）；
   - 启动参数 `-Dtomato-rpc.spi=org.tomato.study.rpc.core.loadbalance.LoadBalance:least-connection`。

4. 验证：`RoundRobinLoadBalance` 里的 `weightMap` 不会被创建，日志里也看不到它的行为——用断点或临时日志确认走了新实现。

加 `ServerHandler` 更简单：实现 `ServerHandler` + 在 `META-INF/services` 加一行即可。

## 常见陷阱

| 陷阱                                           | 现象                                  | 原因                                                      |
| ---------------------------------------------- | ------------------------------------- | --------------------------------------------------------- |
| key 拼错                                       | 启动 NPE                              | `load(key)` 找不到时返回 null，不做兜底                   |
| 忘记放配置文件                                 | NPE 或加载到别的实现                  | 实现类不会自动被扫描，必须有 `META-INF/tomato/{接口全名}` |
| 两端序列化不一致                               | 反序列化失败 / `decode command error` | 帧里只有索引，没有实现名                                  |
| 只改了 `@SpiInterface` 默认值                  | 老配置仍然生效                        | 配置文件里的 key 与注解是两个独立维度                     |
| 依赖 setter 注入但参数是具体类                 | 注入不生效且无提示                    | `injectSpiComponents` 只处理接口类型                      |
| 用了 `singleton=false` 又依赖注入              | 每次 new、每次注入，性能与状态都不同  | 需要自己评估                                              |
| 新增 `Serializer` 但没登记 `META-INF/services` | `SerializerHolder` 里查不到索引       | 两处配置都要改                                            |

## 关键类速查

| 类                                     | 位置                                                                                                                             | 职责                              |
| -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| `SpiLoader`                            | [core/spi](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/spi/SpiLoader.java)                                        | 配置文件扫描、单例、setter 注入   |
| `SpiInterface`                         | [core/spi](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/spi/SpiInterface.java)                                     | 默认 key + 单例标记               |
| `LifeCycle` / `BaseLifeCycleComponent` | [core/observer](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/observer/)                                            | 状态机                            |
| `SerializerHolder`                     | [core/serializer](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/serializer/SerializerHolder.java)                   | 索引 → 序列化器                   |
| `DispatcherHandler`                    | [netty/transport/handler](../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/DispatcherHandler.java) | `ServerHandler` 的 JDK SPI 装配点 |
| `RpcJvmConfigKey`                      | [core](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/RpcJvmConfigKey.java)                                          | `-Dtomato-rpc.*` 全部键名         |
