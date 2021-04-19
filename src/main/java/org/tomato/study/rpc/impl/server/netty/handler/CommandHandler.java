package org.tomato.study.rpc.impl.server.netty.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.CommandType;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@AllArgsConstructor
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<Command> {

    private final HandlerRegistry handlerRegistry;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        CommandType type = CommandType.value(msg.getHeader().getMessageType());
        Optional<ServerHandler> matchHandler = handlerRegistry.match(type);
        if (matchHandler.isEmpty()) {
            ctx.channel().close();
            return;
        }
        Command handleResult = matchHandler.get().handle(msg);
        ctx.channel().writeAndFlush(handleResult).addListener(
                (ChannelFutureListener) listener -> {
                    if (!listener.isSuccess()) {
                        //todo handle exception
                        listener.cause().printStackTrace();
                        ctx.channel().close();
                    }
                });
    }
}
