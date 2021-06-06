package org.tomato.study.rpc.netty.proxy;

import lombok.Setter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.concurrent.ExecutionException;

/**
 * common method of rpc client stub
 * @author Tomato
 * Created on 2021.03.31
 */
public abstract class AbstractStub {

    protected final Serializer serializer = SerializerHolder.getSerializer((byte) 0);

    @Setter
    protected MessageSender messageSender;

    protected Object invokeRemote(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            throw new RuntimeException("rpc methodContext is null");
        }
        Command request = CommandFactory.INSTANCE.requestCommand(
                rpcRequest, serializer, CommandType.RPC_REQUEST);
        try {
            return messageSender.send(request).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
