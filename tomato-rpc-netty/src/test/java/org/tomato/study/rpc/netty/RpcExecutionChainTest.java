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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.ApiConfig;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.netty.service.NettyRpcCoreServiceFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.11.26
 */
public class RpcExecutionChainTest {

    private TestingServer testServer;
    private final int serverPort = 4568;
    private final int rpcPort = 7642;
    private final String stage = "dev";
    private final String group = "default";
    private final TestService testService = numList -> numList.stream().reduce(0, Integer::sum);
    private final String mockMicroServiceId = "DemoRpcServer";
    private RpcCoreService rpcCoreService;

    @Before
    public void init() throws Exception {
        int port = 7642;
        testServer = new TestingServer(port);
        testServer.start();

        rpcCoreService = new NettyRpcCoreServiceFactory().create(
                RpcConfig.builder()
                        .microServiceId(mockMicroServiceId)
                        .nameServiceURI("127.0.0.1:" + rpcPort)
                        .port(serverPort)
                        .stage(stage)
                        .group(group)
                        .businessThreadPoolSize(4)
                        .build()
        );
        rpcCoreService.init();
        rpcCoreService.registerProvider(testService, TestService.class);
        rpcCoreService.start();
    }

    @After
    public void destroy() throws IOException, TomatoRpcException {
        rpcCoreService.stop();
        testServer.stop();
        testServer.close();
    }

    @Test
    public void directRpcTest() throws TomatoRpcException {

        RpcCoreService clientRpcCoreService = new NettyRpcCoreServiceFactory().create(
                RpcConfig.builder()
                        .microServiceId("client")
                        .subscribedServiceIds(Lists.newArrayList(mockMicroServiceId))
                        .nameServiceURI("127.0.0.1:" + rpcPort)
                        .port(serverPort+1)
                        .stage(stage)
                        .group(group)
                        .build()
        );
        clientRpcCoreService.init();
        clientRpcCoreService.start();


        // 测试客户端直连
        TestService directStub = clientRpcCoreService.createDirectStub(
                ApiConfig.<TestService>builder()
                        .api(TestService.class)
                        .microServiceId(mockMicroServiceId)
                        .nodeInfo(MetaData.builder()
                                .microServiceId(mockMicroServiceId)
                                .protocol("tomato")
                                .host("127.0.0.1")
                                .port(serverPort)
                                .stage(stage)
                                .group(group)
                                .build())
                        .build());
        List<Integer> numbers = Lists.newArrayList(1, 2, 3);
        Assert.assertEquals(directStub.sum(numbers), testService.sum(numbers));

        // 测试基于注册中心的调用
        TestService stub = clientRpcCoreService.createStub(
                ApiConfig.<TestService>builder()
                        .microServiceId(mockMicroServiceId)
                        .api(TestService.class)
                        .build());
        Assert.assertEquals(stub.sum(numbers), testService.sum(numbers));

        clientRpcCoreService.stop();
    }
}
