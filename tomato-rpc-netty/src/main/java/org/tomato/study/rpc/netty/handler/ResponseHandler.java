package org.tomato.study.rpc.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.sender.ChannelResponseHolder;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Command> {

    public static final ResponseHandler INSTANCE = new ResponseHandler();

    private final ChannelResponseHolder responseHolder = ChannelResponseHolder.INSTANCE;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        long id = msg.getHeader().getId();
        responseHolder.remove(id).ifPresent(
                nettyResponse -> nettyResponse.getFuture().complete(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
}
