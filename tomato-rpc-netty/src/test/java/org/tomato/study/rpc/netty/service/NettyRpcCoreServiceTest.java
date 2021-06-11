package org.tomato.study.rpc.netty.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.netty.proxy.JdkStubFactory;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;
import org.tomato.study.rpc.core.SpiLoader;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.netty.core.test.TestService;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@RunWith(PowerMockRunner.class)
public class NettyRpcCoreServiceTest {

    @Mock
    private NameService mockNameService;

    @Mock
    private SenderFactory mockSenderFactory;

    @Mock
    private MessageSender mockSender;

    private final Serializer serializer = SerializerHolder.getSerializer((byte) 0);

    private final List<Integer> mockArgs = Arrays.asList(1, 2, 3, 4, 5);

    private final URI mockURI = URI.create("tomato://127.0.0.1:15555");

    /**
     * mock the process:
     * 1.call the client stub
     * 2.the client stub generate the rpc request command
     * 3.the client receive the response and deserialize the data
     * @throws Exception ignore
     */
    @Test
    @PrepareForTest({SpiLoader.class})
    public void clientExecutionChainMockTest() throws Exception {
        mockStatic(SpiLoader.class);
        when(SpiLoader.load(eq(StubFactory.class))).thenReturn(new JdkStubFactory());
        when(SpiLoader.load(eq(SenderFactory.class))).thenReturn(mockSenderFactory);
        when(SpiLoader.load(eq(NameService.class))).thenReturn(mockNameService);
        when(mockNameService.lookupService(any())).thenReturn(mockURI);
        when(mockSenderFactory.create(any())).thenReturn(mockSender);

        // mock netty send
        CompletableFuture<Command> future = new CompletableFuture<>();
        Integer sum = mockArgs.stream().reduce(0, Integer::sum);
        Command mockResponse = CommandFactory.INSTANCE.requestCommand(sum, serializer, CommandType.RPC_RESPONSE);
        future.complete(mockResponse);
        when(mockSender.send(any())).thenReturn(future);

        RpcCoreService rpcService = new NettyRpcCoreService();
        TestService clientStub = rpcService.createStub("mockVip", TestService.class);

        Assert.assertNotNull(clientStub);
        Assert.assertEquals(sum, clientStub.sum(mockArgs));
    }
}
