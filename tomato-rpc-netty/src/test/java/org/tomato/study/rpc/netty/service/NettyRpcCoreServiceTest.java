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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.base.BaseNameService;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.proxy.JdkStubFactory;
import org.tomato.study.rpc.netty.server.NettyRpcServer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({SpiLoader.class, NettyRpcServer.class})
public class NettyRpcCoreServiceTest {

    private NettyRpcCoreService nettyRpcCoreService;

    @Mock
    private ProviderRegistry mockProviderRegistry;

    @Mock
    private BaseNameService mockNameService;

    @Mock
    private NettyRpcServer mockNettyRpcServer;

    private final JdkStubFactory jdkStubFactory = new JdkStubFactory();

    @Before
    public void init() throws Exception {
        mockStatic(SpiLoader.class);
        SpiLoader<ProviderRegistry> providerLoader = (SpiLoader<ProviderRegistry>) mock(SpiLoader.class);
        SpiLoader<NameServerFactory> nameServerLoader = (SpiLoader<NameServerFactory>) mock(SpiLoader.class);
        SpiLoader<StubFactory> stubFactoryLoader = (SpiLoader<StubFactory>) mock(SpiLoader.class);

        NameServerFactory mockNameServerFactory = mock(NameServerFactory.class);
        when(providerLoader.load()).thenReturn(mockProviderRegistry);
        when(nameServerLoader.load()).thenReturn(mockNameServerFactory);
        when(stubFactoryLoader.load()).thenReturn(jdkStubFactory);

        when(SpiLoader.getLoader(eq(ProviderRegistry.class))).thenReturn(providerLoader);
        when(SpiLoader.getLoader(eq(NameServerFactory.class))).thenReturn(nameServerLoader);
        when(SpiLoader.getLoader(eq(StubFactory.class))).thenReturn(stubFactoryLoader);

        when(mockNameServerFactory.createNameService(any())).thenReturn(mockNameService);
        whenNew(NettyRpcServer.class).withAnyArguments().thenReturn(mockNettyRpcServer);

        RpcConfig rpcConfig = RpcConfig.builder()
                .serviceVIP("org.tomato.study.rpc.netty.service.NettyRpcCoreServiceTest")
                .port(1234)
                .build();
        nettyRpcCoreService = new NettyRpcCoreService(rpcConfig);
    }

    /**
     * 测试正常注册provider以及错误传参是否抛出异常
     */
    @Test
    public void registerProviderTest() {
        nettyRpcCoreService.registerProvider(mockNettyRpcServer, RpcServer.class);

        try {
            nettyRpcCoreService.registerProvider(mockNettyRpcServer, NettyRpcServer.class);
        } catch (TomatoRpcRuntimeException e) {
            Assert.assertEquals(
                    e.getMessage(),
                    NettyRpcErrorEnum.CORE_SERVICE_REGISTER_PROVIDER_ERROR.create().getMessage());
            return;
        }
        Assert.fail();
    }

    /**
     * 测试正常创建stub以及异常传参
     */
    @Test
    public void stubCreateTest() {
        Assert.assertNotNull(nettyRpcCoreService.createStub("mock", RpcServer.class));

        try {
            nettyRpcCoreService.createStub("mock", NettyRpcServer.class);
        } catch (TomatoRpcRuntimeException e) {
            Assert.assertEquals(
                    e.getMessage(),
                    NettyRpcErrorEnum.CORE_SERVICE_STUB_CREATE_ERROR.create().getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void startTest() throws Exception {
        doThrow(new Exception("mock error")).when(mockNameService, "subscribe", any(), any());
        try {
            nettyRpcCoreService.init();
            nettyRpcCoreService.start();
        } catch (TomatoRpcException e) {
            Assert.assertEquals(
                    e.getMessage(),
                    NettyRpcErrorEnum.CORE_SERVICE_START_ERROR.create().getMessage());
            return;
        }

        Assert.fail();
    }
}
