# 项目简介
为了巩固微服务基础知识、RPC基础原理而开发的一个的RPC框架。  
项目基于Netty实现了RPC网络通信，并使用Zookeeper作为注册中心实现了简单的服务治理。  
项目参考了dubbo、feign的一些rpc实现思路。  

# 核心类图
![04a28c93a2bb83ee0b0e4f946d8a7f201df85d2b7f075652.png](https://www.imageoss.com/images/2022/02/19/04a28c93a2bb83ee0b0e4f946d8a7f201df85d2b7f075652.png "uml")

# 快速开始

## 依赖检查
jdk版本:openjdk-11  
默认注册中心: zookeeper 3.5.9

## 如何使用
本段以EchoService接口为例，介绍如何通过Tomato-RPC框架，使RPC服务端能够暴露服务接口、 使RPC客户端能够发起RPC调用。  
具体代码见项目的tomato-rpc-sample-api、tomato-rpc-sample-client、  
tomato-rpc-sample-server、tomato-rpc-spring-sample-client、tomato-rpc-spring-sample-server
### 公共jar包
Tomato-RPC的RPC通信是基于接口的， 因此RPC的客户端、服务端需保持接口一致。  
开发RPC程序时，RPC服务端开发者需提供一个公共的jar包，jar包中包含了rpc接口以及接口所需的参数。  
RPC客户端与RPC服务端需共同引入此jar包，保持接口一致性。

接口及方法参数
```java
// TomatoApi注解为框架自定义注解，发布的接口需带上此注解，目的是告诉客户端，发布该接口的服务端的唯一标识
@TomatoApi(microServiceId = "demo-rpc-service")
public interface EchoService {
    String echo(String request);
}
```
发布jar包
```xml
<project>
    <groupId>org.tomato.study.rpc</groupId>
    <artifactId>tomato-rpc-sample-api</artifactId>
    <version>1.0.0</version>
</project>
```

### SpringBoot自动装配方式
#### 服务端配置
引入tomato-rpc-spring-boot-starter
```xml
<dependency>
    <groupId>org.tomato.study.rpc</groupId>
    <artifactId>tomato-rpc-spring-boot-starter</artifactId>
    <!-- 版本以代码为准，下面的版本仅演示用 -->
    <version>1.0.0</version>
</dependency>
```

在spring-boot的application.yaml中配置RPC
```yaml
tomato-rpc:
  # 微服务id
  micro-service-id: "demo-rpc-service"
  # 注册中心地址
  name-service-uri: "127.0.0.1:2181"
  # RPC服务暴露的端口
  port: 4567
  # RPC服务处理线程池数量
  business-thread: 4
  # 服务环境
  stage: "dev"
  # 服务分组
  group: "main"
  # 服务端空闲连接检测时间间隔，单位ms
  server-idle-check-ms: 60000
```

服务端实现RPC接口
```java
// 加上@RpcServerStub，标识当前类是个RPC具体接口实现类
// Spring启动时会将该类加入到IOC容器并注册到RPC接口实现类中
// 实现类的java文件需放在Spring能扫描到的位置
@RpcServerStub
public class EchoServiceImpl implements EchoService {
    @Override
    public String echo(String request) {
        return request;
    }
}
```

#### 客户端配置
同样引入tomato-rpc-spring-boot-starter，并引入api的jar包
```xml
<dependencies>
    <dependency>
        <groupId>org.tomato.study.rpc</groupId>
        <artifactId>tomato-rpc-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.tomato.study.rpc</groupId>
        <artifactId>tomato-rpc-sample-api</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

在spring-boot的application.yaml中配置rpc
```yaml
tomato-rpc:
  # 微服务id
  micro-service-id: "demo-rpc-client"
  # 订阅的其他服务
  subscribed-services:
    - "demo-rpc-service"
  # 注册中心地址
  name-service-uri: "127.0.0.1:2181"
  # RPC服务暴露的端口
  port: 3456
  # RPC服务处理线程池数量
  business-thread: 1
  # 服务环境
  stage: "dev"
  # 服务分组
  group: "main"
  # 客户端发送心跳包的时间间隔，单位ms
  client-keep-alive-ms: 20000
  # 客户端发送数据时是否开启压缩
  use-gzip: false
  # 开启熔断
  enable-circuit: true
  # 错误率超过多少时开启熔断[1, 100]
  circuit-open-rate: 75
  # 断路器开启状态的有效期
  circuit-open-seconds: 60
  # 采样窗口
  circuit-window: 100
```

客户端发起RPC
```java
@Component
public class EchoApiWrapper {

    // 在SpringBean中，添加RPC接口作为成员变量，并加上@RpcClientStub
    // tomato-rpc在Spring启动时会自动将stub注入
    // 使用此注解时，类必须是一个java bean
    // 可配置客户端接口调用的超时时间，单位为毫秒
    @RpcClientStub(timeout = 2000)
    private EchoService echoService;

    public String echo(String msg) {
        return echoService.echo(msg);
    }
}
```

# 特性介绍

## RPC通信
本段将会介绍Tomato-RPC是如何屏蔽底层通信细节，让用户向调用本地方法一样调用远程方法的。

从用户使用层面来说，客户端、服务端通过接口完成了调用方式的约定，而RPC框架会基于动态代理，在客户端为接口创建实例对象。  
客户端在拿到接口实例并调用接口方法时，程序就会走到框架生成的动态代理实例的代码中，剩余的逻辑就全被框架代码接管了。  
所以，框架使用动态代理向客户端屏蔽了底层通信细节，为接口注入框架写好的代码。

从框架层面来说，框架会通过动态代理，拿到用户调用接口方法时的参数对象等关键数据，并将其序列化成可在网络中传输的二进制数据,框架在序列化结束后，就会开始通信逻辑。  
Tomato-RPC是基于TCP进行通信的，如果只有TCP，那么通信双方只能看见无尽的二进制流，接收方不知道什么时候停止数据的接收，无法对数据做有意义的处理。  
因此通信双方需要约定传输层之上的数据格式，使数据有边界，可解析，这就是所谓的应用层通信协议。  
约定好通信协议后，发送方按协议格式发送二进制流，服务端按协议格式解析二进制流，通信边界问题就解决了。

总结一下，Tomato-RPC的通信流程如下：
发送方基于动态代理拦截客户端参数，将其序列化成二进制，并按应用层网络协议封装成帧。  
接收方解析帧，根据发送方的序列化方法反序列化数据，还原调用语义，完成本地调用，并将结果封装成帧，返回给调用方。

### 应用层通信协议
Tomato-RPC设计的应用层通信协议如下：
```text
+-------------+-----------------------+---------+---------------------+---------+-----------+---------+------------+------------+
| magic number| length exclude magic  | version | extension parameter | command | serialize | message | extension  |   body     |
|             | number and this filed |         |      length         |   type  |    type   |   id    | 0 - MaxInt | 0 - MaxInt |
|   1 byte    |       4 bytes         | 4 bytes |      4 bytes        | 2 bytes |   1 byte  | 8 bytes |  bytes     |   bytes    |
+-------------+-----------------------+---------+---------------------+---------+-----------+---------+------------+------------+
```

#### 服务端线程模型
作为服务端，Tomato-RPC建立了三个线程池：Boss线程池、Worker线程池、业务线程池。  
Boss线程池负责调用操作系统的select/epoll，监听server socket accept的可读状况，当accept可读时，Boss线程会调用accept，取出连接，并将连接交给一个Worker线程。  
Worker线程池负责连接的读写、协议的解析(也许可以把协议的解析也放到业务线程池，目前Tomato-RPC没这么做)。   
业务线程池则负责执行暴露给客户端的接口的本机实现方法。

### 连接管理
#### 数据结构
Tomato-RPC的每个[RpcInvoker](https://github.com/CompassA/tomato-rpc/blob/master/tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/invoker/NettyRpcInvoker.java)对象维护了与RPC服务端某个实例的TCP连接。  
客户端会内存中会维护[MicroServiceId -> List\<RpcInvoker\>]这样的映射关系。假设客户端订阅了1个微服务(id="test-service")，该微服务有5个实例，则内存中会有5个与"test-service"相关联的RpcInvoker对象。  
每个RpcInvoker对象组合了一个[NettyClient](https://github.com/CompassA/tomato-rpc/blob/285948ca36ca7861cb0223331d30e71ad39c3a66/tomato-rpc-netty/src/main/java/org/tomato/study/rpc/netty/transport/client/NettyRpcClient.java)对象，NettyClient对象封装了客户端的连接通信逻辑。  
#### 心跳机制
Tomato-RPC的客户端会与RPC服务端的每个实例建立TCP长连接，并根据配置的心跳间隔向服务端发送心跳包(参数: client-keep-alive-ms)。  
而Tomato-RPC的服务端则有空闲连接检测机制，会关闭不活跃的连接(超过一定时间未发消息的连接即为不活跃连接，阈值配置参数: server-idle-check-ms)。  
心跳机制是基于Netty的IdleStateHandler实现的，这里就不赘述其具体原理了。  
#### 优雅关闭
RpcInvoker内部维护了一个计数器，一个标记位。  
计数器记录了当前RpcInvoker被多少个线程调用。  
标记位标记了当前RpcInvoker是否能被调用。 
当RpcInvoker要进行关闭时，首先会将标记为置为false，此时新的想要调用RpcInvoker的线程就会被阻挡住，并收到一个异常。  
设置完标记位后，RpcInvoker会每隔1s检测一次计数器是否为0，只有当计数器为0时，RpcInvoker才会真正关闭连接。  
当然，RpcInvoker也不是无限等待的，当等待时间超过60s后，RpcInvoker就没法"优雅"了，他会强制关闭连接。
[具体逻辑](https://github.com/CompassA/tomato-rpc/blob/master/tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/base/BaseRpcInvoker.java)

### 熔断
Tomato-RPC基于断路器模式实现了一个简单的熔断机制。  
Tomato-RPC以TCP连接为单位进行熔断，当RPC客户端与一个服务的n个实例建立的TCP连接后，Tomato-RPC会创建n个熔断实例，分别统计连接失败率。  

#### 断路器计数器
断路器内部维护一个计数器，计数器记录了断路器所包裹的方法的调用成功次数与失败次数，计数器仅会记录被包裹方法最近n次的调用情况(通过配置采样窗口参数进行控制)。  
Tomato-RPC基于BitSet与FenwickTree实现了一个环状计数器，具体代码见SuccessFailureRingCounter.java。  
每次成功或失败时，会使用BitSet中的一位来记录调用的结果(1代表成功, 0代表失败)，并用FenwickTree维护BitSet的区间和。  
PS: 用FenwickTree当作计算成功次数与失败次数的索引只是为了巩固下这个数据结构，没做过实际的性能测试。。。。  
[具体逻辑](https://github.com/CompassA/tomato-rpc/blob/master/tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/circuit/SuccessFailureRingCounter.java)

#### 断路器状态切换
断路器有三种状态: 关闭、半打开、打开。  
当断路器处于关闭或半打开状态时，请求会被放行，当断路器处于打开状态时，请求会被立马拒绝。
断路器的打开状态有时间限制，当超过设置的时间间隔后，断路器会从打开进入到半打开状态。  
状态间的切换如下所示:  
关闭 ========(失败率超过阈值)=======> 打开 ====(打开状态超时)=====> 半打开  
半打开 =======(调用失败)====> 打开  
半打开 =======(调用成功且当前失败率小于阈值)===========> 关闭
[具体逻辑](https://github.com/CompassA/tomato-rpc/blob/master/tomato-rpc-core/src/main/java/org/tomato/study/rpc/core/circuit/DefaultCircuitBreaker.java)

#### 配置方式
需要配置enable-circuit(是否开启熔断)、circuit-open-rate(错误率阈值)、circuit-open-seconds(断路器打开状态的超时秒数)、circuit-window(采样窗口)四个个参数。
具体配置方式见下文的"快速开始"中的客户端配置。  
若应用是通过Spring接入的，在配置文件配这几个参数即可；若应用是手动接入的，设置RpcConfig类的这三个参数即可。

### 客户端直连服务端调用
Tomato-Rpc支持RPC客户端根据ip、端口、service-id、接口直接构造Stub对象，不依赖与注册中心进行RPC。
```java
@Component
public class DirectRpcTest {
    
    @Autowired
    private RpcCoreService rpcCoreService;
    
    public void test() {
        // 微服务节点信息
        MetaData nodeMeta = MetaData.builder()
                .microServiceId("test")
                .protocol("tomato")
                .host("127.0.0.1")
                .port("5555")
                .stage("dev")
                .group("main")
                .build();
        // 目标接口信息
        ApiConfig<EchoService> apiConfig = ApiConfig.<EchoService>builder()
                // 目标接口
                .api(EchoService.class)
                // 目标服务id
                .microServiceId(mockMicroServiceId)
                // 超时毫秒
                .timeout(10000)
                // 服务的某个具体实例[127.0.0.1:5555]
                .nodeInfo(nodeMata)
                .build();
        // 创建stub
        EchoService directStub = rpcCoreService.createDirectStub(apiConfig);
        // 完成调用
        String response = directStub.echo("hello world");
    }
}

```

## 服务治理

### 服务注册与更新
每个RPC实例都有一个标识自身身份的MicroServiceId。  
一个MicroServiceId就代表一个微服务，一个微服务可能有多个实例节点，这些实例持有相同的MicroServiceId，作为一个整体对外提供服务。  

RPC服务端启动时，会将自身唯一标识、ip、端口上报给注册中心。  
注册中心会维护一个服务目录，记录每个微服务有哪几个实例节点。  
RPC客户端会配置需订阅的RPC服务节点的MicroServiceId，并在启动时向注册中心拉取订阅的微服务的所有的实例节点元数据。  
下图为向注册中心拉取数据的具体流程(基于zookeeper)  
![e66c7fbbcb4434b686ec6e8d0141e4b64b99e457cd1fb957.png](https://www.imageoss.com/images/2022/02/19/e66c7fbbcb4434b686ec6e8d0141e4b64b99e457cd1fb957.png "服务订阅")  
当微服务的实例节点新增/减少时，注册中心会将新增/减少的实例节点的元数据实时下发给订阅该服务的RPC客户端实例。  
下图为服务更新具体流程(基于zookeeper)  
![87cd38165bdc54ffa9fdf30d5e33064a6bcf38553e59b35c.png](https://www.imageoss.com/images/2022/02/19/87cd38165bdc54ffa9fdf30d5e33064a6bcf38553e59b35c.png "服务实例更新")  

RPC服务节点目录结构: /tomato/{micro-service-id}/{stage}/providers/............  
一级目录: Tomato-RPC namespace, 与Tomato-RPC相关的数据都在这个目录中  
二级目录: 各微服务信息  
三级目录: 一个微服务在部署在哪几个环境  
四级目录: 一个微服务在一个环境下的多个元数据（目前只有服务实例信息）  
五级目录: RPC服务实例信息  
![1f9e7eb8fc766ad1fac7de3abb281dcfebbcfb2a7f3bd710.png](https://www.imageoss.com/images/2022/02/19/1f9e7eb8fc766ad1fac7de3abb281dcfebbcfb2a7f3bd710.png "服务树")  

注: 开发时没考虑不同服务，MicroServiceId不慎相同而导致冲突的情况，个人认为，另外建立一个微服务创建中心，专门负责新项目MicroServiceId的分配，是个解决方案。

### 配置stage，实现微服务服务环境隔离
一个微服务可能会部署在不同的环境中，本项目通过两个方式实现环境隔离。
1.微服务连接不同的注册中心，由于不同注册中心的数据互相独立，所以注册在不同注册中心的节点因为无法获取到彼此的元数据而无法通信。
2.微服务使用Tomato-RPC时，配置stage字段，表明自己的服务环境。RPC客户端启动时，仅会订阅stage与自身相同的微服务的元数据。 
例:  
```text
// 启动时配置自身stage为dev
-Dtomato-rpc.service-stage=dev
```

### 配置group，同环境多实例隔离
同一个微服务，在同一个环境中的多个实例，可能部署的代码版本是不同的。  
这种场景在实际使用中很常见:  
1.服务灰度部署，同一个微服务的5个实例，3个部署了旧代码，2个部署了新代码，需要区分旧实例与灰度实例。  
2.内网环境联调测试，在微服务内网集群中，部署一个自己开发的新功能的测试环境，测试在验证时，通过一些配置将流量引入测试环境。  

用户在Tomato-RPC注册微服务时可配置group字段，标识自己的微服务属于哪一个分组。  
Tomato-RPC的RPC客户端默认会向group字段与自己相同的其他RPC服务实例发起RPC调用。  

例: service-id为"test-a"的服务的group是"alpha"，它想调用group为"dev"的服务B(service-id:"test-b")的实例、group为"sit"的服务C(service-id:"test-c")的实例。  
那么在服务A启动时，可以加上jvm参数来实现调用。  
```text
// 启动时配置自身group为alpha
-Dtomato-rpc.service-group=alpha  
// 启动时配置调用test-b的group dev中的实例、test-c的group sit中的实例
-Dtomato-rpc.subscribed-services-group=test-b:dev&test-c:sit
```

## SPI
Tomato-RPC实现了一个简单的SPI，每个组件通过SpiLoader加载依赖的组件，用户可通过添加SPI配置文件、配置JVM参数的方式替换组件实现而无需改变代码。

### 代码样例
```java
/**
 * 通过注解标识这是一个SPI接口，"jdk"为配置文件中的key
 * 程序将加载配置文件中"jdk"对应的实现类
 */
@SpiInterface("jdk")
public interface StubFactory {
    <T> T createStub(StubConfig<T> config);
}
```

在项目的资源路径下添加"META-INF/tomato"目录, SPI会读取这个目录下的SPI配置  
添加一个文件，名字为SPI接口类全名: org.tomato.study.rpc.core.StubFactory  
每行添加SPI配置参数，形式为"参数名:具体实现类的类全名"
```text
jdk : org.tomato.study.rpc.netty.proxy.JdkStubFactory
```

配置完毕后通过SpiLoader加载接口实现类
```java
public class SpiDemo {
    // 通过spi的方式加载StubFactory组件
    private final StubFactory stubFactory = SpiLoader.getLoader(StubFactory.class).load();
}
```
### 依赖注入
当一个SPI接口实现类依赖了其他SPI组件时,Tomato-Rpc的SPI会尝试依赖注入。  
Tomato-Rpc的SPI会检测当前SPI接口实现类的所有Setter方法,若Setter方法的入参也是SPI接口,  
会继续加载Setter入参对应的SPI接口组件，并通过反射，调用Setter其注入到当前SPI实现类中。


Tomato-Rpc的SPI对单例对象的循环依赖做了处理,若SPI接口被配置为单例,Tomato-Rpc的SPI会在注入依赖前,先将SPI组件缓存至一个Map中。
```java
@SpiInterface("a")
public interface SpiInterfaceA {
    SpiInterfaceB getB();
}

@SpiInterface("b")
public interface SpiInterfaceB {
    SpiInterfaceC getC();
}

@SpiInterface("c")
public interface SpiInterfaceC {
    SpiInterfaceA getA();
}

@NoArgsConstructor
public class SpiInterfaceAImpl implements SpiInterfaceA {
    @Setter
    @Getter
    private SpiInterfaceB b;
}

@NoArgsConstructor
public class SpiInterfaceBImpl implements SpiInterfaceB {
    @Getter
    @Setter
    private SpiInterfaceC c;
}

@NoArgsConstructor
public class SpiInterfaceCImpl implements SpiInterfaceC {
    @Setter
    @Getter
    private SpiInterfaceA a;
}


class SpiLoopInjectTest {
    /**
     * 测试循环依赖
     */
    @Test
    public void loopDependencyTest() {
        SpiInterfaceA a = SpiLoader.getLoader(SpiInterfaceA.class).load();
        Assert.assertTrue(a instanceof SpiInterfaceAImpl);
        Assert.assertTrue(a.getB() instanceof SpiInterfaceBImpl);
        Assert.assertTrue(a.getB().getC() instanceof SpiInterfaceCImpl);
        Assert.assertTrue(a.getB().getC().getA() instanceof SpiInterfaceAImpl);
    }
}

```
### jvm参数配置spi
-Dtomato-rpc.spi=spi接口类全名1:组件key1&spi接口类全名2:组件key2

## 监控信息
Tomato-RPC提供了Restful接口，供用户查询服务内部状态。
当一个SpringBoot应用启动时，Tomato-RPC会注册一个Controller，专门用来暴露服务内部数据。

### 服务invoker信息
```text
GET /tomato/status/invoker?service-id=demo-rpc-service

Param
    service-id: 要查询的微服务的唯一标识
```
这个接口可用于查询服务订阅的微服务有多少个节点，接口会按照stage、group对invoker数据进行分组，返回当前服务订阅的微服务的所有实例信息。返回的json如下所示。
```json
[
  {
    "stage": "dev",
    "groups": {
      "main": [
        {
          "protocol": "tomato",
          "host": "192.168.0.163",
          "port": 4567,
          "microServiceId": "demo-rpc-service",
          "stage": "dev",
          "group": "main"
        }
      ],
      "local-test": [
        {
          "protocol": "tomato",
          "host": "192.168.0.164",
          "port": 4568,
          "microServiceId": "demo-rpc-service",
          "stage": "dev",
          "group": "local-test"
        }
      ]
    }
  },
  {
    "stage": "prod",
    "groups": {
      "main": [
        {
          "protocol": "tomato",
          "host": "192.168.0.163",
          "port": 4567,
          "microServiceId": "demo-rpc-service",
          "stage": "dev",
          "group": "main"
        }
      ]
    }
  }
]
```
## 路由
todo

## 均衡负载
目前基于随机策略，从一个微服务的多个实例节点中随机选取一个发起调用。  
todo 后续增加多种方式

# k8s部署样例

## 搭建zookeeper
首先，在k8s集群搭建单节点的zookeeper。  
```yaml
apiVersion: v1
kind: Service
metadata:
  namespace: tomato
  name: zookeeper
  labels:
    name: zookeeper
spec:
  clusterIP: None
  ports:
    - name: zookeeper-port
      port: 2181
  selector:
    app: zookeeper

---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  namespace: tomato
  name: zookeeper-set
spec:
  serviceName: zookeeper
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      namespace: tomato
      labels:
        app: zookeeper
    spec:
      containers:
      - name: zookeeper
        image: zookeeper:3.5.9
        ports:
          - containerPort: 2181
        volumeMounts:
          - name: data-pvc
            mountPath: /data 
  volumeClaimTemplates:
    - metadata:
        namespace: tomato
        name: data-pvc
      spec:
        storageClassName: manual
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 1Gi
```
上面的代码创建了一个stateful-set, 配合headless-service使得集群其他节点可使用dns的方式,  
用"zookeeper-set-0.zookeeper.tomato.svc.cluster.local"来访问zookeeper。  

## 部署demo-server服务
demo-server是一个样例服务，会将任何客户端发送的rpc请求数据echo回去，并将服务节点的ip、stage、group等数据信息也发送会客户端。  
(代码见repo的tomato-rpc-spring-sample-server)
将tomato-rpc-spring-sample-server制作成镜像，并部署至k8s。
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  namespace: tomato
  labels:
    app: rpc-sample-deployment
  name: rpc-sample-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rpc-sample-server-pod
  strategy: {}
  template:
    metadata:
      namespace: tomato
      labels:
        app: rpc-sample-server-pod
    spec:
      containers:
      - image: compassa/rpc-sample-server:1.0.0
        env:
          - name: "JAVA_OPTIONS"
            value: "-Dtomato-rpc.name-service-uri=zookeeper-set-0.zookeeper.tomato.svc.cluster.local:2181"
        imagePullPolicy: IfNotPresent
        name: rpc-sample-server
        stdin: true
        tty: true
        ports:
          - name: rpc-port
            containerPort: 1535
            protocol: TCP
```

查看是否部署成功
```bash
#根据标签搜索pod
> kubectl get pods --namespace=tomato -l app=rpc-sample-server-pod
#查询到了结果
NAME                                     READY   STATUS    RESTARTS   AGE
rpc-sample-deployment-79f49bd6db-9lhx8   1/1     Running   0          26s
rpc-sample-deployment-79f49bd6db-djs7x   1/1     Running   0          20m
rpc-sample-deployment-79f49bd6db-trclt   1/1     Running   0          26s


```

去集群中的zookeeper检查服务是否注册成功
```bash
# 本地连接到zookeeper容器
> kubectl exec -it zookeeper-set-0 -n tomato -- bash
# 容器中检查数据是否完成
root@zookeeper-set-0:/apache-zookeeper-3.5.9-bin# zkCli.sh
[zk: localhost:2181(CONNECTED) 6] ls /tomato/demo-rpc-service/dev/providers
[tomato%3A%2F%2F10.42.219.80%3A4567%2F%3Fmicro-service-id%3Ddemo-rpc-service%26stage%3Ddev%26group%3Dmain]
```

## 部署demo client

tomato-rpc-spring-sample-client会不停的向demo-service发送rpc请求。(代码见repo的tomato-rpc-spring-sample-client)  
将tomato-rpc-spring-sample-client制作成镜像，并部署至k8s。
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sample-client-job
  namespace: tomato
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rpc-client-demo
  template:
    metadata:
      namespace: tomato
      name: sample-client
      labels:
        app: rpc-client-demo
    spec:
      containers:
        - name: sample-client
          image: compassa/sample-client:1.0.0
          imagePullPolicy: IfNotPresent
          stdin: true
          tty: true
          env:
            - name: "JAVA_OPTIONS"
              value: "-Dtomato-rpc.name-service-uri=zookeeper-set-0.zookeeper.tomato.svc.cluster.local:2181"
```

查看日志，若有下面的响应信息，即部署成功，到这里，所有链路都打通了。
```bash
> kubectl logs sample-client-job-66d658f76f-9x6qq -n tomato -f
#查看到了响应日志
2022-01-09 14:19:22.187  INFO 8 --- [pool-5-thread-1] o.t.s.r.s.s.c.DemoClientApplication      : hello client!
request message: DemoRequest{data='hello world', testMap=[{a=1, b=2, c=3}, {c=4, d=5, e=6}, {e=7, f=8, g=9}], testList=[1, 2, 3, 4]}
provider host address: 10.42.219.84
provider micro-service-id: demo-rpc-service
provider stage: dev
provider group: main

```