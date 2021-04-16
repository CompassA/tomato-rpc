package org.tomato.study.rpc.impl.codec.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.tomato.study.rpc.core.protocol.Command;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyFrameEncoder extends MessageToMessageEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Command command,
                          List<Object> list) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        Command.encode(command, buffer);
        ctx.channel().writeAndFlush(buffer);
    }
}
