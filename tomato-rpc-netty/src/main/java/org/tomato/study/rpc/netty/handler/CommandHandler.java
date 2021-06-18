package org.tomato.study.rpc.netty.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.netty.data.RpcResponse;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<Command> {

    private final ConcurrentMap<CommandType, ServerHandler> providerMap = new ConcurrentHashMap<>(0);

    public CommandHandler() {
        ServiceLoader<ServerHandler> serviceHandlers = ServiceLoader.load(ServerHandler.class);
        for (ServerHandler serviceHandler : serviceHandlers) {
            register(serviceHandler);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        try {
            CommandType type = CommandType.value(msg.getHeader().getMessageType());
            Optional<ServerHandler> matchHandler = match(type);
            if (matchHandler.isEmpty()) {
                throw new IllegalStateException("rpc server handler not found, type: " + type);
            }
            Command handleResult = matchHandler.get().handle(msg);
            ctx.channel().writeAndFlush(handleResult).addListener(
                    (ChannelFutureListener) listener -> {
                        if (!listener.isSuccess()) {
                            listener.cause().printStackTrace();
                            ctx.channel().writeAndFlush(
                                    CommandFactory.INSTANCE.response(
                                            msg.getHeader().getId(),
                                            RpcResponse.fail(new TomatoRpcException("netty response write failed")),
                                            SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                                            CommandType.RPC_RESPONSE
                                    )
                            );
                            ctx.channel().close();
                        }
                    });
        } catch (Exception exception) {
            exception.printStackTrace();
            ctx.channel().writeAndFlush(
                    CommandFactory.INSTANCE.response(
                            msg.getHeader().getId(),
                            RpcResponse.fail(new TomatoRpcException("netty response write failed")),
                            SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                            CommandType.RPC_RESPONSE)
            );
            ctx.channel().close();
        }
    }

    private Optional<ServerHandler> match(CommandType type) {
        return Optional.ofNullable(providerMap.get(type));
    }

    private void register(ServerHandler serverHandler) {
        providerMap.put(serverHandler.getType(), serverHandler);
    }
}
