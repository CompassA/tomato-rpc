/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty;

import com.google.common.collect.Lists;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.ExtensionHeader;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.router.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.netty.service.NettyRpcCoreServiceFactory;
import org.tomato.study.rpc.test.GrayRouterTestFacade;
import org.tomato.study.rpc.test.GrayRouterTestFacadeImpl;
import org.tomato.study.rpc.test.TestService;
import org.tomato.study.rpc.test.TestServiceImpl;
import org.tomato.study.rpc.test.TimeoutTest;
import org.tomato.study.rpc.test.TimeoutTestImpl;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 集成测试
 * @author Tomato
 * Created on 2021.11.26
 */
public class RpcExecutionChainTest {

    private TestingServer testServer;
    private final int serverPort = 23456;
    private final int grayPort = 23457;
    private final int serverDownstreamPort = 23458;
    private final int grayDownstreamPort = 23459;
    private int clientPort = 23422;
    private final int zkPort = 7642;
    private final String stage = "dev";
    private final String grayGroup = "gray";
    private final String group = "default";
    private final long sleepMs = 10000;
    private final TestService testService = new TestServiceImpl();
    private final TimeoutTest timeoutTestService = new TimeoutTestImpl(sleepMs, testService);

    private final String mockMicroServiceId = "DemoRpcServer";
    private final String mockDownstreamMicroServiceId = "DownstreamRpcServer";
    private RpcCoreService rpcCoreService;
    private RpcCoreService grayRpcCoreService;
    private RpcCoreService grayDownStreamRpcCoreService;
    private RpcCoreService prodDownStreamRpcCoreService;
    private RpcCoreService clientRpcCoreService;
    private final List<Integer> numbers = Lists.newArrayList(1, 2, 3);

    @Before
    public void init() throws Exception {
        // init zk
        testServer = new TestingServer(zkPort);
        testServer.start();

        // init prod downstream
        prodDownStreamRpcCoreService = new NettyRpcCoreServiceFactory().create(
            RpcConfig.builder()
                .microServiceId(mockDownstreamMicroServiceId)
                .nameServiceURI("127.0.0.1:" + zkPort)
                .port(serverDownstreamPort)
                .stage(stage)
                .group(group)
                .businessThreadPoolSize(4)
                .build()
        );

        GrayRouterTestFacadeImpl prodDownstreamFacade = new GrayRouterTestFacadeImpl();
        prodDownstreamFacade.setId("prodDownstreamFacade");
        prodDownstreamFacade.setRpcServerMetaData(prodDownStreamRpcCoreService.getRpcServerMetaData());

        prodDownStreamRpcCoreService.init();
        prodDownStreamRpcCoreService.registerProvider(prodDownstreamFacade, GrayRouterTestFacade.class);
        prodDownStreamRpcCoreService.start();

        // init gray downstream
        grayDownStreamRpcCoreService = new NettyRpcCoreServiceFactory().create(
            RpcConfig.builder()
                .microServiceId(mockDownstreamMicroServiceId)
                .nameServiceURI("127.0.0.1:" + zkPort)
                .port(grayDownstreamPort)
                .stage(stage)
                .group(grayGroup)
                .businessThreadPoolSize(4)
                .build()
        );

        GrayRouterTestFacadeImpl grayDownstreamFacade = new GrayRouterTestFacadeImpl();
        grayDownstreamFacade.setId("grayDownstreamFacade");
        grayDownstreamFacade.setRpcServerMetaData(grayDownStreamRpcCoreService.getRpcServerMetaData());

        grayDownStreamRpcCoreService.init();
        grayDownStreamRpcCoreService.registerProvider(grayDownstreamFacade, GrayRouterTestFacade.class);
        grayDownStreamRpcCoreService.start();

        // init prod
        rpcCoreService = new NettyRpcCoreServiceFactory().create(
                RpcConfig.builder()
                        .microServiceId(mockMicroServiceId)
                        .subscribedServiceIds(Lists.newArrayList(mockDownstreamMicroServiceId))
                        .nameServiceURI("127.0.0.1:" + zkPort)
                        .port(serverPort)
                        .stage(stage)
                        .group(group)
                        .businessThreadPoolSize(4)
                        .build()
        );
        rpcCoreService.init();

        GrayRouterTestFacadeImpl prodFacade = new GrayRouterTestFacadeImpl();
        prodFacade.setId("prodFacade");
        prodFacade.setRpcServerMetaData(rpcCoreService.getRpcServerMetaData());

        rpcCoreService.registerProvider(testService, TestService.class);
        rpcCoreService.registerProvider(timeoutTestService, TimeoutTest.class);
        rpcCoreService.registerProvider(prodFacade, GrayRouterTestFacade.class);
        rpcCoreService.start();

        prodFacade.setStub(rpcCoreService.createStub(
                new StubConfig<>(
                        GrayRouterTestFacade.class,
                        mockDownstreamMicroServiceId,
                        group,
                        false,
                        10000L,
                        rpcCoreService.getNameServer())
        ));

        // init gray
        grayRpcCoreService = new NettyRpcCoreServiceFactory().create(
                RpcConfig.builder()
                        .microServiceId(mockMicroServiceId)
                        .subscribedServiceIds(Lists.newArrayList(mockDownstreamMicroServiceId))
                        .nameServiceURI("127.0.0.1:" + zkPort)
                        .port(grayPort)
                        .stage(stage)
                        .group(grayGroup)
                        .businessThreadPoolSize(4)
                        .build()
        );

        GrayRouterTestFacadeImpl grayFacade = new GrayRouterTestFacadeImpl();
        grayFacade.setId("grayFacade");
        grayFacade.setRpcServerMetaData(grayRpcCoreService.getRpcServerMetaData());

        grayRpcCoreService.init();
        grayRpcCoreService.registerProvider(grayFacade, GrayRouterTestFacade.class);
        grayRpcCoreService.start();

        grayFacade.setStub(grayRpcCoreService.createStub(
                new StubConfig<>(
                        GrayRouterTestFacade.class,
                        mockDownstreamMicroServiceId,
                        group,
                        false,
                        10000L,
                        grayRpcCoreService.getNameServer())
        ));

        // init client
        clientRpcCoreService = new NettyRpcCoreServiceFactory().create(
                RpcConfig.builder()
                        .microServiceId("client")
                        .subscribedServiceIds(Lists.newArrayList(mockMicroServiceId))
                        .nameServiceURI("127.0.0.1:" + zkPort)
                        .enableCircuit(true)
                        .port(++clientPort)
                        .stage(stage)
                        .group(group)
                        .build()
        );
        clientRpcCoreService.init();
        clientRpcCoreService.start();
    }

    @After
    public void destroy() throws IOException, TomatoRpcException {
        rpcCoreService.stop();
        clientRpcCoreService.stop();
        grayRpcCoreService.stop();
        grayDownStreamRpcCoreService.stop();
        prodDownStreamRpcCoreService.stop();
        testServer.stop();
        testServer.close();
    }

    /**
     * 测试客户端直连
     */
    @Test
    public void directRpcTest() {
        MetaData.NodeProperty p = new MetaData.NodeProperty();
        p.weight = 1;
        MetaData nodeInfo = MetaData.builder()
                .microServiceId(mockMicroServiceId)
                .protocol("tomato")
                .host("127.0.0.1")
                .port(serverPort)
                .stage(stage)
                .group(group)
                .nodeProperty(p)
                .build();
        StubConfig<TestService> stubConfig = new StubConfig<>(
                TestService.class,
                mockMicroServiceId,
                group,
                true,
                10000L,
                nodeInfo);
        TestService directStub = clientRpcCoreService.createStub(stubConfig);
        List<Integer> numbers = Lists.newArrayList(1, 2, 3);
        assertEquals(directStub.sum(numbers), testService.sum(numbers));
    }

    /**
     * 测试基于注册中心的调用
     */
    @Test
    public void nameServerRouterRpcTest() throws InterruptedException {
        StubConfig<TestService> stubConfig = new StubConfig<>(
                TestService.class,
                mockMicroServiceId,
                group,
                true,
                10000L,
                clientRpcCoreService.getNameServer());
        TestService stub = clientRpcCoreService.createStub(stubConfig);
        assertEquals(stub.sum(numbers), testService.sum(numbers));
    }

    /**
     * 灰度路由测试
     */
    @Test
    public void nameServerRouterGrayTest() {
        StubConfig<GrayRouterTestFacade> stubConfig = new StubConfig<>(
                GrayRouterTestFacade.class,
                mockMicroServiceId,
                group,
                true,
                10000L,
                clientRpcCoreService.getNameServer());
        GrayRouterTestFacade stub = clientRpcCoreService.createStub(stubConfig);

        // 未设置灰度规则, 全部走prod-default
        InvocationContext.initContext();
        InvocationContext.put("USER_ID", "1");
        MDC.put(ExtensionHeader.TRACE_ID.name(), ExtensionHeader.TRACE_ID.getValueFromContext());
        try {
            Optional<MicroServiceSpace> opt = clientRpcCoreService.getNameServer().getMicroService(mockMicroServiceId);
            assertTrue(opt.isPresent());

            MicroServiceSpace microServiceSpace = opt.get();
            microServiceSpace.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID + 1, List.of(String.format("USER_ID %% 10 == 2 -> group == \"%s\"", grayGroup)));

            List<String> request = List.of(MetaData.convert(clientRpcCoreService.getRpcServerMetaData()).get().toASCIIString());
            List<String> pass = stub.pass(request);
            List<MetaData> metadata = pass.stream()
                .map(URI::create)
                .map(MetaData::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            assertEquals(3, metadata.size());
            assertTrue(metadata.stream().anyMatch(m -> Objects.equals(m.getPort(), clientPort)));
            assertTrue(metadata.stream().anyMatch(m -> Objects.equals(m.getPort(), serverPort)));
            assertTrue(metadata.stream().anyMatch(m -> Objects.equals(m.getPort(), serverDownstreamPort)));
        } finally {
            InvocationContext.remove();
            MDC.clear();
        }


        // 设置路由规则, client为prod环境, 下游链路全走灰度
        InvocationContext.initContext();
        InvocationContext.put("USER_ID", "1");
        MDC.put(ExtensionHeader.TRACE_ID.name(), ExtensionHeader.TRACE_ID.getValueFromContext());
        try {
            Optional<MicroServiceSpace> opt = clientRpcCoreService.getNameServer().getMicroService(mockMicroServiceId);
            assertTrue(opt.isPresent());

            MicroServiceSpace microServiceSpace = opt.get();
            microServiceSpace.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID + 2, List.of(String.format("USER_ID %% 10 == 1 -> group == \"%s\"", grayGroup)));

            List<String> grayPass = stub.pass(List.of(
                MetaData.convert(clientRpcCoreService.getRpcServerMetaData()).get().toASCIIString()));
            List<MetaData> grayTestMetadataList = grayPass.stream()
                .map(URI::create)
                .map(MetaData::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            assertEquals(3, grayTestMetadataList.size());
            assertTrue(grayTestMetadataList.stream().anyMatch(m -> Objects.equals(m.getPort(), clientPort)));
            assertTrue(grayTestMetadataList.stream().anyMatch(m -> Objects.equals(m.getPort(), grayPort)));
            assertTrue(grayTestMetadataList.stream().anyMatch(m -> Objects.equals(m.getPort(), grayDownstreamPort)));
        } finally {
            InvocationContext.remove();
            MDC.clear();
        }
    }

    /**
     * 直连RPC调用超时测试
     */
    @Test
    public void directRpcTimeoutTest() throws InterruptedException {
        MetaData.NodeProperty p = new MetaData.NodeProperty();
        p.weight = 1;
        MetaData nodeInfo = MetaData.builder()
                .microServiceId(mockMicroServiceId)
                .protocol("tomato")
                .host("127.0.0.1")
                .port(serverPort)
                .stage(stage)
                .group(group)
                .nodeProperty(p)
                .build();
        StubConfig<TimeoutTest> stubConfig = new StubConfig<>(
                TimeoutTest.class,
                mockMicroServiceId,
                group,
                false,
                sleepMs / 2L,
                nodeInfo
        );
        TimeoutTest directStub = clientRpcCoreService.createStub(stubConfig);
        boolean hasTimeout = false;
        try {
            directStub.sum(numbers);
        } catch (TomatoRpcRuntimeException timeout) {
            hasTimeout = TomatoRpcErrorEnum.RPC_INVOCATION_TIMEOUT.getCode() == timeout.getErrCode().getCode();
        }
        assertTrue(hasTimeout);

        // 防止服务端RpcCoreService提前关闭导致单测报错
        Thread.sleep(sleepMs);
    }

    /**
     * 基于注册中心调用超时测试
     */
    @Test
    public void nameServerRouterTimeoutTest() throws InterruptedException {
        StubConfig<TimeoutTest> stubConfig = new StubConfig<>(
                TimeoutTest.class,
                mockMicroServiceId,
                group,
                false,
                sleepMs / 2L,
                clientRpcCoreService.getNameServer());
        TimeoutTest stub = clientRpcCoreService.createStub(stubConfig);
        boolean hasTimeout = false;
        try {
            stub.sum(numbers);
        } catch (TomatoRpcRuntimeException timeout) {
            hasTimeout = TomatoRpcErrorEnum.RPC_INVOCATION_TIMEOUT.getCode() == timeout.getErrCode().getCode();
        }
        assertTrue(hasTimeout);

        // 防止服务端RpcCoreService提前关闭导致单测报错
        Thread.sleep(sleepMs);
    }
}
