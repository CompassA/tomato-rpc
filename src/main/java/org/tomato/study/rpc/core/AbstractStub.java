package org.tomato.study.rpc.core;

import lombok.Setter;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.CommandFactory;
import org.tomato.study.rpc.core.protocol.CommandType;
import org.tomato.study.rpc.data.MethodContext;

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

    protected Object invokeRemote(MethodContext methodContext) {
        if (methodContext == null) {
            throw new RuntimeException("rpc methodContext is null");
        }
        Command request = CommandFactory.INSTANCE.createRequest(
                methodContext, serializer, CommandType.RPC_REQUEST);
        try {
            return messageSender.send(request).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
