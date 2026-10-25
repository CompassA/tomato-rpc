# tomato-rpc-common

> 最底层的工具模块：5 个类，零框架依赖，任何模块都能安全引用。

## 清单

| 类             | 位置                                                                                                             | 作用                                                                       |
| -------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `Logger`       | [common/utils](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/utils/Logger.java)              | slf4j 的枚举门面，两个 logger：`TOMATO-RPC-DEFAULT` 与 `TOMATO-RPC-DIGEST` |
| `NetworkUtil`  | [common/utils](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/utils/NetworkUtil.java)         | 取本机可用 IP、拼 URI                                                      |
| `ReflectUtils` | [common/utils](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/utils/ReflectUtils.java)        | `reflectSet` / `reflectGet`，异常只打日志返回 null                         |
| `FenwickTree`  | [common/structure](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/structure/FenwickTree.java) | 树状数组，维护区间和                                                       |
| `ApiUrl`       | [common/monitor](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/monitor/ApiUrl.java)          | 4 个本地监控接口路径常量                                                   |

## Logger

`enum Logger implements org.slf4j.Logger`——用枚举实现「两个固定 logger + 常量」：

```java
public enum Logger implements org.slf4j.Logger {
    DEFAULT(LoggerFactory.getLogger("TOMATO-RPC-DEFAULT")),
    DIGEST(LoggerFactory.getLogger("TOMATO-RPC-DIGEST"));

    public static final String SUCCESS_MARK = "Y";
    public static final String FAILURE_MARK = "N";
}
```

两个 logger 的分工：

| logger               | 内容                                          | 格式             |
| -------------------- | --------------------------------------------- | ---------------- |
| `TOMATO-RPC-DEFAULT` | 生命周期、注册订阅、路由刷新、异常堆栈        | 人类可读         |
| `TOMATO-RPC-DIGEST`  | 单行摘要，如 `\|refresh-router\|Y\|3\|1234\|` | 便于 grep / 统计 |

因为实现了完整的 `org.slf4j.Logger` 接口（所有重载方法全部委托给内部 logger），所以可以像普通 logger 一样用 `Logger.DEFAULT.info("a={}", a)`，也可以把一个 `Logger` 直接当 `org.slf4j.Logger` 传出去。

`SUCCESS_MARK`/`FAILURE_MARK` 是摘要行里统一的 `Y`/`N` 标记——想给某段逻辑加摘要日志时照着 `|操作名|Y/N|耗时|参数|` 的格式写，晚上做聚合统计时省事。

## NetworkUtil

注册到 ZK 的 host 就是这里算出来的，逻辑是「**第一个非回环、非虚拟、已启用、带 IPv4 的网卡地址**」：

```java
while (interfaces.hasMoreElements()) {
    NetworkInterface ni = interfaces.nextElement();
    if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) { continue; }   // 跳过 lo、docker 虚拟网卡、down
    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
        if (addr.isLoopbackAddress()) { continue; }
        if (IP_PATTERN.matcher(host).matches() && !"0.0.0.0".equals(host)) { return addr; }
    }
}
return null;   // 最终兜底 127.0.0.1
```

| 注意点                                      | 影响                                                 |
| ------------------------------------------- | ---------------------------------------------------- |
| 只认 IPv4（正则 `\d{1,3}(\.\d{1,3}){3,5}`） | 纯 IPv6 环境下会退化成 `127.0.0.1`                   |
| 按网卡枚举顺序取第一个                      | 多网卡/容器里可能取到不是「对外可达」的那块          |
| 取不到时返回 `127.0.0.1`                    | 跨机器部署会连不上，日志里能看到注册的是 `127.0.0.1` |

`createURI(protocol, host, port)` 只拼 `protocol://host:port`，**不带 path 和 query**——`MetaData` 的完整 URL 是自己拼的（见 [注册中心](./tomato-rpc-registry-zookeeper.md)）。

## ReflectUtils

```java
public static <T> void reflectSet(T instance, Class<T> clazz, String fieldName, Object value) {
    try {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(instance, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
        Logger.DEFAULT.error(e.getMessage(), e);   // 吞掉，不抛
    }
}
```

只在 `SpiLoader` 的 setter 注入和测试里用。**异常被吞**是刻意的（注入失败不影响主流程），但也意味着字段名写错时只会看到一条 error 日志、程序继续跑。

## FenwickTree

熔断计数窗口的底座。`SuccessFailureRingCounter` 用 `BitSet` 存最近 N 次调用结果，再用 `FenwickTree` 维护「前 i 位有多少个 1」，于是「窗口内失败数」可以 O(log n) 拿到：

```text
BitSet:      1 0 1 1 0 0 1 ...   （1=成功，0=失败）
FenwickTree: 前缀和 → 任意区间 [l, r] 内的成功数 = sum(r) - sum(l-1)
```

实现在 [FenwickTree](../../tomato-rpc-common/src/main/java/org/tomato/study/rpc/common/structure/FenwickTree.java)，测试在 `FenwickTreeTest`。作者在 README 里也承认：「用 FenwickTree 当索引只是为了巩固这个数据结构，没做过实际性能测试」。

## ApiUrl

```java
INVOKER_STATUS  = "/api/tomato/invoker/local/status"     // 订阅到的实例（按 stage→group 分组）
INVOKER_READY   = "/api/tomato/invoker/local/ready"      // 就绪探针
ROUTER_LIST     = "/api/tomato/router/local/list"        // 本机生效的路由规则
ROUTER_REFRESH  = "/api/tomato/router/local/refresh"     // dashboard 下发的入口
```

被 `MonitorController`（config 模块）和 `ZookeeperRegistryDAO`（dashboard 模块）同时引用——**这是 common 存在的最大意义**：两个应用共享同一批路径常量，改路径只需改一处。

## 依赖

只依赖 `slf4j-api` 与 `commons-lang3`（`StringUtils`）。不依赖 Spring、Netty、ZK——所以它出现在所有模块的依赖树里也不会引入冲突。
