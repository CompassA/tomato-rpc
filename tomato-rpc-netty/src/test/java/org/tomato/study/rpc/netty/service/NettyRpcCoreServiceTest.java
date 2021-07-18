/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.core.test.TestService;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * todo: refactor
 * @author Tomato
 * Created on 2021.04.17
 */
@RunWith(PowerMockRunner.class)
public class NettyRpcCoreServiceTest {

    @Test
    public void t() {

    }

//    @Mock
//    private NameService mockNameService;
//
//    @Mock
//    private SpiLoader<NameService> mockNameServiceLoader;
//
//    private int port;
//
//    private RpcCoreService rpcCoreService;
//
//    @Before
//    public void init() {
//        this.rpcCoreService = new NettyRpcCoreService(
//                RpcConfig.builder()
//                        .serviceVIP("mockVIP")
//                        .subscribedVIP(Collections.emptyList())
//                        .nameServiceURI("mock")
//                        .build()
//        );
//        this.port = 1234;
//    }
//
//    /**
//     * mock the process:
//     * 1.call the client stub
//     * 2.the client stub generate the rpc request command
//     * 3.the client receive the response and deserialize the data
//     * @throws Exception ignore
//     */
//    @Test
//    @PrepareForTest({SpiLoader.class})
//    public void executionChainTest() throws Exception {
//        // mock name service
//        mockStatic(SpiLoader.class);
//        when(SpiLoader.getLoader(eq(NameService.class))).thenReturn(mockNameServiceLoader);
//        when(SpiLoader.getLoader(eq(NameService.class))).thenReturn(mockNameServiceLoader);
//        when(mockNameServiceLoader.load()).thenReturn(mockNameService);
//        when(mockNameServiceLoader.load()).thenReturn(mockNameService);
//        when(SpiLoader.getLoader(eq(ProviderRegistry.class))).thenCallRealMethod();
//
//        //1. export rpc server
//        RpcServer rpcServer = rpcCoreService.startRpcServer(port);
//        String serviceVIP = "mockVIP";
//        TestService serverService = new TestServiceImpl();
//
//        //2. mock name service look up
//        URI serviceURI = rpcCoreService.registerProvider(serverService, TestService.class);
//        when(mockNameService.lookupService(any())).thenReturn(Optional.of(serviceURI));
//
//        //3. create stub
//        TestService clientStub = rpcCoreService.createStub(serviceVIP, TestService.class);
//
//        List<Integer> testList = Arrays.stream(new Integer[]{1, 2, 3, 4}).collect(Collectors.toList());
//        Assert.assertEquals(clientStub.sum(testList), serverService.sum(testList));
//
//        rpcServer.close();
//        Assert.assertTrue(rpcServer.isClosed());
//    }
//
//    public static class TestServiceImpl implements TestService {
//
//        @Override
//        public Integer sum(List<Integer> nums) {
//            if (nums == null) {
//                return 0;
//            }
//            return nums.stream().reduce(0, Integer::sum);
//        }
//    }


}
