# 通信协议

> 本篇是网络通信模块的第一篇：**一帧数据长什么样**。想知道这帧数据怎么被收发、被谁处理，继续读 [调用链路](./05-invocation.md) 与 [Netty 传输层](./06-transport.md)。

## 设计目标

| 目标     | 落地方式                                                                  |
| -------- | ------------------------------------------------------------------------- |
| 通用     | 不假设 body 的序列化方式，帧头里带 `serializeType`                        |
| 可扩展   | 固定头之后留一段「扩展头」，用于超时、压缩、链路 ID 等旁路参数，不占 body |
| 便于拆包 | 第二个字段就是长度，直接喂给 Netty 的 `LengthFieldBasedFrameDecoder`      |
| 可复用   | 请求与响应共用同一套帧结构，只靠 `messageType` 区分；心跳也是同一套帧     |

协议对象定义在 core（与 Netty 无关）：[Header](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/Header.java)、[Command](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/Command.java)、[ProtoConstants](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/ProtoConstants.java)。Netty 只是把这套对象和字节之间做映射。

## 帧结构

```text
 0        1                        5              9              13      14      15                23
 +--------+------------------------+--------------+--------------+-------+-------+-----------------+-----------+---------+
 | magic  |        length          |   version    | extensionLen |  type |  ser  |       id        | extension |  body   |
 |  1B    |         4B             |      4B      |      4B      |  2B   |  1B   |       8B        |  extLen B | bodyLen |
 +--------+------------------------+--------------+--------------+-------+-------+-----------------+-----------+---------+
    0xac       19+extLen+bodyLen       20210403      扩展头字节数    见下表   见下表   消息ID（8字节）     kv 明文     序列化数据
```

| 字段              | 起始偏移  | 长度               | Java 类型 | 写入         | 读取        | 说明                                                            |
| ----------------- | --------- | ------------------ | --------- | ------------ | ----------- | --------------------------------------------------------------- |
| `magic`           | 0         | 1                  | `byte`    | `writeByte`  | `readByte`  | 固定 `0xac`，解码时校验                                         |
| `length`          | 1         | 4                  | `int`     | `writeInt`   | `readInt`   | **不含** magic 与自身，等于 `19 + extensionLength + bodyLength` |
| `version`         | 5         | 4                  | `int`     | `writeInt`   | `readInt`   | 固定 `20210403`，当前**不校验**                                 |
| `extensionLength` | 9         | 4                  | `int`     | `writeInt`   | `readInt`   | 扩展头字节数，0 表示没有                                        |
| `messageType`     | 13        | 2                  | `short`   | `writeShort` | `readShort` | 见 [CommandType](#commandtype)                                  |
| `serializeType`   | 15        | 1                  | `byte`    | `writeByte`  | `readByte`  | 序列化器索引：`0`=protostuff，`1`=json                          |
| `id`              | 16        | 8                  | `long`    | `writeLong`  | `readLong`  | 请求 ID；响应原样带回，用于匹配 future                          |
| `extension`       | 24        | `extensionLength`  | `byte[]`  | `writeBytes` | `readBytes` | UTF-8 的 `k:v&k:v` 明文                                         |
| `body`            | 24+extLen | `length-19-extLen` | `byte[]`  | `writeBytes` | `readBytes` | 业务序列化数据                                                  |

`HEAD_FIX_LENGTH = 19` 只统计**除 magic 与 length 外**的固定字段：`4(version) + 4(extensionLength) + 2(type) + 1(serializeType) + 8(id)`。所以：

```text
整帧字节数 = 1 + 4 + length
length     = 19 + extensionLength + bodyLength
bodyLength = length - 19 - extensionLength
```

### 为什么 length 要这么设计

`NettyFrameDecoder` 的构造参数决定了这个语义：

```java
public NettyFrameDecoder() {
    super(
        Integer.MAX_VALUE, // maxFrameLength
        1,                 // lengthFieldOffset：跳过 magic
        4,                 // lengthFieldLength：length 是 4 字节
        0,                 // lengthAdjustment：长度字段值直接就是剩余长度
        0);                // initialBytesToStrip：不剥离，整帧交给下游
}
```

因为 `initialBytesToStrip = 0`，下游 `NettyProtoDecoder` 拿到的是**含 magic 和 length 在内的完整帧**，于是 `NettyCommandCodec.decode` 可以从头开始按固定顺序读全部字段（见 [NettyCommandCodec](../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyCommandCodec.java)）。

## CommandType

[CommandType](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/CommandType.java) 决定「谁能处理这一帧」：

| 值  | 枚举                  | 方向            | body                       | 处理方                        |
| --- | --------------------- | --------------- | -------------------------- | ----------------------------- |
| 0   | `UNKNOWN`             | —               | 无                         | 只打 warn                     |
| 1   | `RPC_REQUEST`         | client → server | `RpcRequestDTO` 序列化结果 | `RpcRequestHandler`           |
| 2   | `RPC_RESPONSE`        | server → client | `RpcResponse` 序列化结果   | `ResponseHandler`             |
| 3   | `KEEP_ALIVE_REQUEST`  | client → server | 无（body 为空）            | `KeepAliveRequestHandler`     |
| 4   | `KEEP_ALIVE_RESPONSE` | server → client | 无                         | `ResponseHandler`（仅记日志） |

> 服务端和客户端拿的是各自 `ServerHandler` / 分支，但帧结构完全一致，因此一次心跳和一次业务请求在传输层没有任何特殊对待。

## 扩展头

扩展头是一个 UTF-8 明文串，格式与 JVM 参数一致：`k1:v1&k2:v2`（分隔符常量在 `RpcJvmConfigKey.ENTRY_DELIMITER` / `KEY_VALUE_DELIMITER`）。内置键定义在 [ExtensionHeader](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/ExtensionHeader.java)：

| key | 枚举       | 类型      | 谁写                                                                    | 语义                                           |
| --- | ---------- | --------- | ----------------------------------------------------------------------- | ---------------------------------------------- |
| `1` | `TIMEOUT`  | `Long`    | 客户端（`BaseStubInvoker#putParameter` 写入 `StubConfig.timeoutMs`）    | 本次调用超时 ms；客户端时间轮读它决定超时时间  |
| `2` | `COMPRESS` | `Boolean` | 客户端（stub 的 `compressBody`）；服务端在压缩过的**响应**上回写 `true` | 该帧 body 是否 gzip                            |
| `3` | `TRACE_ID` | `String`  | 客户端（`InvocationContext.initContext` 生成 32 位 UUID）               | 链路 ID，服务端放入 `InvocationContext` 与 MDC |

除内置键外，**整个 `InvocationContext` 都会被写进扩展头**：

```java
// NettyRpcInvoker#doInvoke
Map<String, String> contextMap = InvocationContext.get();
Command rpcRequest = CommandFactory.request(invocation, getSerializer(), contextMap, CommandType.RPC_REQUEST);
```

这正是灰度路由能工作的前提：业务侧 `InvocationContext.put("USER_ID", "...")` → 扩展头 → 服务端 `InvocationContext.set(extensionHeaders)` → `ExprRouter.matchRequest` 用 `USER_ID` 求值。链路里每一跳都会把上下文继续往下带。

### 扩展头与长度的联动

扩展头不是独立存储的，它的字节数会同步进 `Header`：

```java
// ExtensionHeaderBuilder#build
byte[] newExtHeaders = builder.toString().getBytes(StandardCharsets.UTF_8);
command.setExtension(newExtHeaders);
header.setLength(header.getLength() - header.getExtensionLength() + newExtHeaders.length);
header.setExtensionLength(newExtHeaders.length);
```

同理 `CommandFactory#changeBody` 会在替换 body 时修正 `length`（压缩/解压时使用）。**任何改动 body 或 extension 的地方都必须走这两个方法**，否则编码阶段会抛 `CODEC_ENCODE_ERROR`：

- `extension.length != extensionLength` → `frame extension length error`
- `body.length != length - extensionLength - 19` → `frame body length error`

### 已知限制

1. 没有转义：值里如果出现 `:` 或 `&` 会被 `parseMultiKeyValue` 截断（它要求 `split(":")` 后正好 2 段），这类值应避免放进上下文。
2. 无长度上限：扩展头是明文，没有单条大小限制，塞大对象会显著增加帧体积。
3. `TIMEOUT` 只对客户端有意义，服务端不读它，也没有 deadline 传播机制。

## body 与序列化

| 方向 | body 类型                            | 关键字段                                                                               |
| ---- | ------------------------------------ | -------------------------------------------------------------------------------------- |
| 请求 | `RpcRequestDTO`（实现 `Invocation`） | `microServiceId`、`interfaceName`、`methodName`、`argsTypes[]`、`returnType`、`args[]` |
| 响应 | `RpcResponse`（实现 `Response`）     | `code`、`message`、`data`                                                              |

服务端收到请求后，会把 `RpcRequestDTO` 经 `ConvertUtils` 转成 `RpcRequestModel`（补上 `rpcInterFace` = `Class.forName(interfaceName)`），再用 `microServiceId + interfaceName` 从 `ProviderRegistry` 取实现对象，最后 `interfaceClass.getMethod(methodName, argsTypes).invoke(provider, args)`。

`serializeType` 决定用哪个序列化器，查表逻辑在 [SerializerHolder](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/serializer/SerializerHolder.java)：

```java
static {
    ServiceLoader.load(Serializer.class)
        .forEach(serializer -> SERIALIZER_MAP.put(serializer.serializerIndex(), serializer));
}
```

| index | 实现                   | 特点                                                                            |
| ----- | ---------------------- | ------------------------------------------------------------------------------- |
| 0     | `ProtostuffSerializer` | 二进制、体积小，需要类结构稳定                                                  |
| 1     | `JsonSerializer`       | 可读性好；开启 `activateDefaultTyping`，JSON 中写入类型信息，反序列化能还原多态 |

注意：SPI 的默认 key 是 `json`（`@SpiInterface("json")`），但 `SerializerHolder` 走的是 JDK `ServiceLoader` + `serializerIndex()` 查表，两条路互不影响——**帧里的 `serializeType` 才是决定性字段**。

## 零依赖的编解码

编码就是按字段顺序写：

```java
// NettyCommandCodec#encode
byteBuf.writeByte(header.getMagicNumber());
byteBuf.writeInt(length);
byteBuf.writeInt(header.getVersion());
byteBuf.writeInt(extensionLength);
byteBuf.writeShort(header.getMessageType());
byteBuf.writeByte(header.getSerializeType());
byteBuf.writeLong(header.getId());
```

解码就是按同样顺序读，并顺手校验：

| 校验项                                | 失败后果                                             |
| ------------------------------------- | ---------------------------------------------------- |
| `header == null`                      | `IllegalStateException("command header is null")`    |
| `magic != 0xac`                       | `IllegalStateException("magic number is not valid")` |
| `length < 0` 或 `extensionLength < 0` | `IllegalStateException("illegal length")`            |

`NettyProtoDecoder#exceptionCaught` 会打印 `decode command error` 后 `ctx.close()`——**一旦收到的字节流无法解释，直接断开这条连接**，不做纠错重试。

## 一个具体例子

设一次 `echo` 请求：`id = 7`，`serializeType = 1`（json），无扩展头，body 42 字节。

```text
length = 19 + 0 + 42 = 61 (0x3D)
整帧 = 1 + 4 + 61 = 66 字节
```

```text
AC                   magic
00 00 00 3D          length = 61
01 34 62 E3          version = 20210403
00 00 00 00          extensionLength = 0
00 01                messageType = 1 (RPC_REQUEST)
01                   serializeType = 1 (json)
00 00 00 00 00 00 00 07   id = 7
<42 字节 body>        RpcRequestDTO 的 JSON
```

如果把 `USER_ID=2323` 放进上下文，扩展头就是 `1:5000&2:false&3:9f1c...&USER_ID:2323`（`BaseStubInvoker` 会写 `1`/`2`，`initContext` 会写 `3`），假设这段 UTF-8 为 45 字节，则 `extensionLength = 45`、`length = 19 + 45 + 42 = 106`，`extension` 段紧跟 `id` 之后。

> 扩展头是明文，直接 `cat` 抓包就能看到 traceId 和业务上下文——调试方便，生产上要注意别把敏感信息塞进去。

## 兼容性与演进

| 现状                       | 影响                                     | 若要演进                            |
| -------------------------- | ---------------------------------------- | ----------------------------------- |
| `version` 不校验           | 新老协议混跑不会立刻报错，可能静默错解析 | 在 `NettyProtoDecoder` 加版本白名单 |
| 固定头无「状态码」         | 业务错误只能放在 `RpcResponse.code` 里   | 新增字段必须靠 `version` 分支解析   |
| 未知 `messageType` 只 warn | 老版本客户端收到新类型帧会丢弃而不是报错 | 可回一个 `UNSUPPORTED` 响应         |
| 同一连接无多路复用上限     | 依靠 `id` 匹配，理论上不限并发           | ——                                  |

## 关键类速查

| 类                                                              | 位置                                                                                                           | 职责                                    |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `Header`                                                        | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/Header.java)                       | 帧头的 Java 表示                        |
| `Command`                                                       | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/Command.java)                      | header + extension + body               |
| `CommandFactory`                                                | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/CommandFactory.java)               | 构造请求/响应帧、替换 body、生成自增 id |
| `CommandType`                                                   | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/CommandType.java)                  | 消息类型枚举                            |
| `ExtensionHeader` / `ExtensionHeaderBuilder`                    | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/ExtensionHeaderBuilder.java)       | 扩展头读写与长度联动                    |
| `ProtoConstants`                                                | [core/data](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/data/ProtoConstants.java)               | magic / version / 固定头长度            |
| `NettyCommandCodec`                                             | [netty/codec](../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/NettyCommandCodec.java)       | 字节 ↔ `Command`                        |
| `NettyFrameDecoder` / `NettyProtoDecoder` / `NettyFrameEncoder` | [netty/codec](../tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/codec/)                             | 拆包、校验、整帧编码                    |
| `SerializerHolder`                                              | [core/serializer](../tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/serializer/SerializerHolder.java) | 按索引查序列化器                        |
