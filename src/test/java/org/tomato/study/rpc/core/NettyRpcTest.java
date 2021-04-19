package org.tomato.study.rpc.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.test.TestService;
import org.tomato.study.rpc.impl.proxy.JdkStubFactory;
import org.tomato.study.rpc.impl.rpc.NettyRpcCoreService;
import org.tomato.study.rpc.impl.sender.netty.NettySenderFactory;
import org.tomato.study.rpc.impl.server.netty.NettyRpcServer;
import org.tomato.study.rpc.impl.server.netty.handler.RpcRequestHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
@RunWith(PowerMockRunner.class)
public class NettyRpcTest {

    @Mock
    private NameService mockNameService;

    @Test
    @PrepareForTest({SpiLoader.class})
    public void executionChainTest() throws Exception {
        mockStatic(SpiLoader.class);
        when(SpiLoader.load(eq(StubFactory.class))).thenReturn(new JdkStubFactory());
        when(SpiLoader.load(eq(SenderFactory.class))).thenReturn(new NettySenderFactory());
        when(SpiLoader.load(eq(NameService.class))).thenReturn(mockNameService);
        when(SpiLoader.load(eq(RpcServer.class))).thenReturn(new NettyRpcServer());
        RpcRequestHandler rpcRequestHandler = new RpcRequestHandler();
        when(SpiLoader.load(eq(ProviderRegistry.class))).thenReturn(rpcRequestHandler);
        when(SpiLoader.loadAll(eq(ServerHandler.class))).thenReturn(Collections.singletonList(rpcRequestHandler));

        RpcCoreService rpcCoreService = new NettyRpcCoreService();
        RpcServer rpcServer = rpcCoreService.startRpcServer(15678);
        String serviceVIP = "mockVIP";
        TestService serverService = new TestServiceImpl();
        URI serviceURI = rpcCoreService.registerProvider(serviceVIP, serverService, TestService.class);
        when(mockNameService.lookupService(any())).thenReturn(serviceURI);

        TestService clientStub = rpcCoreService.createStub(serviceVIP, TestService.class);

        List<Integer> testList = Arrays.asList(1, 2, 3, 4);
        Assert.assertEquals(clientStub.sum(testList), serverService.sum(testList));

        rpcServer.close();
        Assert.assertTrue(rpcServer.isClosed());
    }

    public static class TestServiceImpl implements TestService {

        @Override
        public Integer sum(List<Integer> nums) {
            if (nums == null) {
                return 0;
            }
            return nums.stream().reduce(0, Integer::sum);
        }
    }


}
