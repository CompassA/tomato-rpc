# tomato-rpc-netty

> 网络通信模块，也是整个框架**唯一**真正跑在网络上的部分。core 定义了「怎么调用」的抽象，这个模块回答「字节怎么进出网卡」。

## 职责

| 它做什么                                            | 它不做什么                                      |
| --------------------------------------------------- | ----------------------------------------------- |
| 把 `Command` 编成字节帧、把字节帧解成 `Command`     | 定义协议语义（`Header`、`CommandType` 在 core） |
| 管理服务端的 boss/worker/business 三组线程          | 决定路由、负载均衡、熔断（core 的职责）         |
| 管理客户端的连接（建连、重连、心跳、关闭）          | 管理注册中心（registry-zookeeper 的职责）       |
| 把请求从 IO 线程切到业务线程池执行本地实现          | 序列化实现（core 的 `Serializer`）              |
| 组装 `RpcCoreService` 的 Netty 版本（SPI 默认实现） | 提供 Spring 装配（config 的职责）               |

## 依赖

```text
tomato-rpc-netty
├── tomato-rpc-core               // 全部抽象
├── tomato-rpc-registry-zookeeper // 默认注册中心实现（主动依赖，见下方说明）
├── io.netty:netty-all            // 4.1.97.Final
├── io.protostuff:*               // 默认序列化
└── com.fasterxml.jackson         // JSON 序列化
```

一个容易意外的设计：**netty 模块直接依赖了 registry-zookeeper**。所以业务方只要引 `tomato-rpc-netty` 就自动带上 ZK 注册中心，不需要单独引；反过来 `registry-zookeeper` 不依赖 netty（它只用 core 的接口）。这就是样例模块 `tomato-rpc-sample-server` 只声明一个依赖就能跑起来的原因。

## 包结构

```text
org.tomato.study.rpc.netty
├── codec/         4 个编解码器 + 2 个帧层原始实现
├── transport/
│   ├── server/    NettyRpcServer
│   ├── client/    NettyRpcClient、ChannelWrapper、NettyResponseHolder、NettyResponseFuture
│   └── handler/   7 个 ChannelHandler
├── invoker/       NettyRpcInvoker、NettyRpcInvokerFactory
├── interceptor/   CompressInterceptor
├── router/        NettyMicroServiceSpace（把 core 的空间和 invoker 工厂接起来）
├── service/       NettyRpcCoreService、NettyRpcCoreServiceFactory（SPI 入口）
├── data/          NettyInvocationResult
└── utils/         ConvertUtils（DTO ↔ Model）
```

## 类清单

### codec（编解码）

| 类                                                                                                                                                                                                                            | 职责                                                                                                                    |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| [NettyFrameDecoder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyFrameDecoder.java)                                                                                                             | 继承 `LengthFieldBasedFrameDecoder(MAX, 1, 4, 0, 0)`：跳过 1 字节 magic，用第 1～4 字节的 `length` 切帧，**不剥离帧头** |
| [NettyFrameEncoder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyFrameEncoder.java)                                                                                                             | `MessageToByteEncoder<Command>`，把 `Command` 交给 `NettyCommandCodec` 写进 `ByteBuf`                                   |
| [NettyProtoDecoder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyProtoDecoder.java)                                                                                                             | 继承 `ByteToMessageDecoder`，把完整帧解析成 `Command`（`NettyCommandCodec#decode`）                                     |
| [NettyCommandCodec](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyCommandCodec.java)                                                                                                             | 真正的字节级实现：18 个 `ProtoConstants` 偏移常量 + `encode`/`decode` + 半包判定                                        |
| [RawFrameDecoder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/RawFrameDecoder.java) / [RawFrameEncoder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/RawFrameEncoder.java) | 只做「帧头 ↔ `Header`」的原始层，被 `CodecTest` 直接使用，也便于单独测帧格式                                            |

细节（字段偏移、`length` 语义、19 字节帧头）见 [协议](./../04-protocol.md)。

### transport

| 类                                                                                                                               | 职责                                                                                                                    |
| -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| [NettyRpcServer](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/server/NettyRpcServer.java)           | 继承 `BaseRpcServer`：`doInit` 建线程池 + Bootstrap + Pipeline，`doStart` 只 `bind().sync()`，`doStop` 优雅关闭三组线程 |
| [NettyRpcClient](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/NettyRpcClient.java)           | 一个客户端 = 一条长连接。负责懒建连（双检锁）、`send`、重连、关闭                                                       |
| [ChannelWrapper](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/ChannelWrapper.java)           | 对 `Channel` 的极薄包装（`isActiveChannel` / `closeChannel`），让 core 的接口不暴露 Netty 类型                          |
| [NettyResponseHolder](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/NettyResponseHolder.java) | `ConcurrentHashMap<Long, NettyResponseFuture>`，消息 id → 等待中的 future                                               |
| [NettyResponseFuture](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/NettyResponseFuture.java) | 包一层 `CompletableFuture<Command>` + `messageId` + `timeStamp`（超时任务用）                                           |

### handler（Pipeline 里的 7 个节点）

| 类                                                                                                                                        | 在哪一侧 | 干什么                                                                                                                                            |
| ----------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| [ServerIdleCheckHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/ServerIdleCheckHandler.java)   | 服务端   | `IdleStateHandler(readerIdle)`，读空闲触发后**直接关连接**（不发探测包）                                                                          |
| [DispatcherHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/DispatcherHandler.java)             | 服务端   | 用 JDK SPI 加载所有 `ServerHandler` 建 `CommandType → handler` 映射；`RPC_REQUEST` 切业务线程池；写 `InvocationContext`/MDC；持有一组写死的拦截器 |
| [RpcRequestHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/RpcRequestHandler.java)             | 服务端   | 真正的业务执行：反序列化参数 → `ConvertUtils` → `providerRegistry.getProvider` → 反射 `invoke` → 组装 `RpcResponse`                               |
| [KeepAliveRequestHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/KeepAliveRequestHandler.java) | 服务端   | 收到 `KEEP_ALIVE_REQUEST` 回 `KEEP_ALIVE_RESPONSE`                                                                                                |
| [ClientIdleCheckHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/ClientIdleCheckHandler.java)   | 客户端   | `IdleStateHandler(writerIdle)`，写空闲触发时发心跳                                                                                                |
| [KeepAliveHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/KeepAliveHandler.java)               | 客户端   | 收到写空闲事件后主动 `writeAndFlush` 一个 `KEEP_ALIVE_REQUEST`                                                                                    |
| [ResponseHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/ResponseHandler.java)                 | 客户端   | 收到 `RPC_RESPONSE`：按需解压 body → `responseHolder.getAndRemove(id).complete(command)`                                                          |

两条 Pipeline 的实际顺序：

```text
服务端  idle-checker → frame-decoder → proto-decoder → frame-encoder → dispatcher-handler
客户端  idle-checker → frame-decoder → proto-decoder → frame-encoder → response-handler
```

`NettyFrameEncoder` 必须放在 decoder 之后但在 handler 之前——它负责出站方向，位置只影响 `writeAndFlush` 的遍历顺序。

### invoker / 其余

| 类                                                                                                                                    | 职责                                                                                                                    |
| ------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| [NettyRpcInvoker](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/invoker/NettyRpcInvoker.java)                       | 一个实例一个 invoker。持有一个 `NettyRpcClient`、一个 100ms 精度的 `HashedWheelTimer`；`doInvoke` 里组帧 + 注册超时任务 |
| [NettyRpcInvokerFactory](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/invoker/NettyRpcInvokerFactory.java)         | `RpcInvokerFactory` 的 Netty 实现（SPI key `netty`）                                                                    |
| [CompressInterceptor](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/interceptor/CompressInterceptor.java)           | 服务端 gzip 解压请求体、压缩响应体，并回写扩展头 `2=true`                                                               |
| [NettyMicroServiceSpace](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/router/NettyMicroServiceSpace.java)          | 继承 `BaseMicroServiceSpace`，只负责按 `MetaData` 造 `NettyRpcInvoker`                                                  |
| [NettyRpcCoreService](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/service/NettyRpcCoreService.java)               | `BaseRpcCoreService` 的 Netty 版：覆写 `createStub`/`registerProvider`/`createMicroServiceSpace`/`createRpcServer`      |
| [NettyRpcCoreServiceFactory](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/service/NettyRpcCoreServiceFactory.java) | SPI 入口，`key = netty`                                                                                                 |
| [NettyInvocationResult](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/data/NettyInvocationResult.java)              | 把收到的 `Command` 转成 core 的 `Result`                                                                                |
| [ConvertUtils](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/utils/ConvertUtils.java)                               | `RpcRequestDTO` ↔ `RpcRequestModel`，内部带 `CLASS_CACHE` 反射缓存                                                      |

## 线程模型

```text
                    ┌─────────────── rpc-server-boss-thread (1 个)
accept ────────────►│
                    └─────────────── rpc-server-worker-thread (CPU*2)
                                        │ 解码 + 分发
                                        ▼
                              businessThreadPoolSize > 1 ?
                                   │              │
                                 是│              │否（默认 1）
                                   ▼              ▼
                      rpc-server-business-thread   直接在 worker 线程执行
                      ArrayBlockingQueue(5000)
                      AbortPolicy + error 日志
```

`DispatcherHandler` 是唯一做线程切换的地方：

```java
protected void channelRead0(ChannelHandlerContext ctx, Command msg) {
    ServerHandler matchHandler = handlerMap.get(CommandType.value(msg.getHeader().getMessageType()));
    if (matchHandler == null) { return; }

    if (type == CommandType.RPC_REQUEST && businessExecutor != null) {
        businessExecutor.execute(() -> processRequest(ctx, msg, matchHandler));  // 切池
        return;
    }
    processRequest(ctx, msg, matchHandler);   // 没有池就留在 worker 线程
}
```

注意 `processRequest` 里的 `ctx.writeAndFlush` 是线程安全的（Netty 会从当前线程排队回 eventLoop），所以业务线程里直接回包不需要额外处理。

客户端侧没有业务线程池：调用方线程自己阻塞在 `future.get(timeout)` 上，IO 完成由 worker 线程回调 `ResponseHandler` 唤醒。

## 一次调用的字节之旅

```text
[客户端调用线程]
  BaseStubInvoker → RouterStubInvoker → CircuitRpcInvoker → NettyRpcInvoker#doInvoke
     ├─ 从 InvocationContext 取超时/压缩/traceId 写扩展头
     ├─ serializer 序列化参数 → CommandFactory.createRequestCommand(typeId, serializerIndex, body)
     ├─ responseHolder.put(id, future)
     ├─ timer.newTimeout(RpcTimeoutTask, timeout, MS)
     └─ NettyRpcClient#send → channel.writeAndFlush(command)
                                  ▼
                         [客户端 worker 线程]
                    NettyFrameEncoder → NettyCommandCodec#encode
                                  ▼  ── TCP ──
                         [服务端 worker 线程]
                    NettyFrameDecoder（按 length 切帧）
                    NettyProtoDecoder → NettyCommandCodec#decode
                                  ▼
                         DispatcherHandler（切业务线程）
                                  ▼
                         RpcRequestHandler
                    serializer 反序列化 → getProvider → method.invoke
                                  ▼
                         ResponseHandler  ← 回程，对称
```

## 扩展点

| 扩展方式                        | 位置                                                                                                                                                                   |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 换 `RpcInvokerFactory` 实现     | `META-INF/tomato/org.tomato.study.rpc.core.invoker.RpcInvokerFactory` 当前是 `netty: ...NettyRpcInvokerFactory`                                                        |
| 换 `RpcCoreServiceFactory` 实现 | `tomato-rpc-config` 里的同名文件指定 `netty: ...NettyRpcCoreServiceFactory`                                                                                            |
| 新增服务端处理逻辑              | 实现 core 的 `ServerHandler`，再写进 `META-INF/services/org.tomato.study.rpc.core.ServerHandler`（JDK SPI，当前注册了 `RpcRequestHandler`、`KeepAliveRequestHandler`） |
| 加/换请求拦截                   | 实现 core 的 `CommandInterceptor` 后，改 `DispatcherHandler` 构造函数里那个写死的 `interceptors` 数组（当前只有 `CompressInterceptor`，没有 SPI 化）                   |
| 换传输层                        | 整个模块可被替换：core 只依赖 `RpcClient`/`RpcServer`/`RpcInvoker` 等接口                                                                                              |

## 调优参数

| 参数                   | 默认                                        | 调大的后果                                          | 调小的后果                        |
| ---------------------- | ------------------------------------------- | --------------------------------------------------- | --------------------------------- |
| `business-thread`      | `1`                                         | 并发高但线程多了 GC/切换开销；≤1 时**不启用**线程池 | 无意义（0 或 1 都走 worker 线程） |
| `server-idle-check-ms` | `600000`                                    | 死连接回收慢                                        | 误杀空闲长连接                    |
| `client-keep-alive-ms` | `serverIdle/3`                              | 心跳少、省流量                                      | 心跳多；必须 < 服务端空闲阈值     |
| `SO_BACKLOG`           | `10000`（硬编码）                           | 瞬时握手堆积可承受                                  | —                                 |
| 业务队列               | `5000`（硬编码）                            | 缓冲更多请求，但会增加平均延迟                      | 更容易触发拒绝                    |
| 连接超时               | `10000ms`（硬编码 `CONNECTION_TIMEOUT_MS`） | 弱网更稳                                            | 快速失败                          |
| 全局调用超时           | `50000ms`（无 stub 配置时）                 | —                                                   | —                                 |

## 已知问题

这些问题在 [传输层](./../06-transport.md) 里有完整分析，模块层面记住结论即可：

| #   | 问题                                                                                        |
| --- | ------------------------------------------------------------------------------------------- |
| 1   | `send` 先 `writeAndFlush` 再在监听器里 `putFeatureResponse`，极快响应可能落空（本该先登记） |
| 2   | 没有 `channelInactive` 清理 `NettyResponseHolder`，连接断开后未完成的 future 只能等超时     |
| 3   | 每个 `NettyRpcInvoker` 各自 new 一个 `HashedWheelTimer`，实例多时线程/定时任务偏重          |
| 4   | 压缩无阈值，小 body 也 gzip（可能变大）                                                     |
| 5   | 业务线程池拒绝时只记日志，客户端不会收到明确错误，只能等超时                                |
| 6   | 协议 `version` 字段没有校验                                                                 |
| 7   | 客户端不检测读空闲，服务端静默断连后要等下一次写失败才发现                                  |
| 8   | 不支持 TLS/鉴权                                                                             |

## 阅读顺序

1. [NettyRpcCoreService](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/service/NettyRpcCoreService.java) + [NettyRpcCoreServiceFactory](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/service/NettyRpcCoreServiceFactory.java)：先看入口怎么被 SPI 装配。
2. [NettyRpcServer](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/server/NettyRpcServer.java)：再看服务端怎么起。
3. [NettyCommandCodec](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyCommandCodec.java)：字节格式是理解一切的地基。
4. [DispatcherHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/DispatcherHandler.java) → [RpcRequestHandler](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/handler/RpcRequestHandler.java)：请求怎么变成方法调用。
5. [NettyRpcInvoker](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/invoker/NettyRpcInvoker.java) + [NettyRpcClient](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/NettyRpcClient.java)：客户端怎么发、怎么等、怎么超时。
6. [CompressInterceptor](../../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/interceptor/CompressInterceptor.java)：最后一个横切关注点。

配套章节：[应用层协议](./../04-protocol.md)、[调用链路](./../05-invocation.md)、[传输层](./../06-transport.md)。
