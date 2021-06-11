package org.tomato.study.rpc.netty.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.HandlerRegistry;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.RpcServerFactory;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.SpiLoader;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.netty.core.test.TestService;
import org.tomato.study.rpc.netty.proxy.JdkStubFactory;
import org.tomato.study.rpc.netty.sender.NettySenderFactory;
import org.tomato.study.rpc.netty.server.NettyRpcServerFactory;
import org.tomato.study.rpc.netty.server.handler.RpcRequestHandler;
import org.tomato.study.rpc.netty.service.NettyRpcCoreService;

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
 * @author Tomato
 * Created on 2021.04.17
 */
@RunWith(PowerMockRunner.class)
public class NettyRpcTest {

    @Mock
    private NameService mockNameService;

    @Mock
    private HandlerRegistry handlerRegistry;

    private final int port = 1234;

    @Test
    @PrepareForTest({SpiLoader.class})
    public void executionChainTest() throws Exception {
        mockStatic(SpiLoader.class);
        when(SpiLoader.load(eq(StubFactory.class))).thenReturn(new JdkStubFactory());
        when(SpiLoader.load(eq(SenderFactory.class))).thenReturn(new NettySenderFactory());
        when(SpiLoader.load(eq(NameService.class))).thenReturn(mockNameService);
        when(SpiLoader.load(eq(RpcServerFactory.class))).thenReturn(new NettyRpcServerFactory());
        RpcRequestHandler rpcRequestHandler = new RpcRequestHandler();
        when(handlerRegistry.match(any())).thenReturn(Optional.of(rpcRequestHandler));
        when(SpiLoader.load(eq(ProviderRegistry.class))).thenReturn(rpcRequestHandler);
        when(SpiLoader.loadAll(eq(ServerHandler.class))).thenReturn(Collections.singletonList(rpcRequestHandler));

        //1. export rpc server
        RpcCoreService rpcCoreService = new NettyRpcCoreService();
        RpcServer rpcServer = rpcCoreService.startRpcServer(port);
        String serviceVIP = "mockVIP";
        TestService serverService = new TestServiceImpl();

        //2. mock name service look up
        URI serviceURI = rpcCoreService.registerProvider(serviceVIP, serverService, TestService.class);
        when(mockNameService.lookupService(any())).thenReturn(serviceURI);

        //3. create stub
        TestService clientStub = rpcCoreService.createStub(serviceVIP, TestService.class);

        List<Integer> testList = Arrays.stream(new Integer[]{1, 2, 3, 4}).collect(Collectors.toList());
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
