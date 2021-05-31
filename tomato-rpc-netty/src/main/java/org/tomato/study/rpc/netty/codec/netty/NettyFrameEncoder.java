package org.tomato.study.rpc.netty.codec.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.utils.CommandUtil;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyFrameEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Command command,
                          ByteBuf out) throws Exception {
        CommandUtil.encode(command, out);
    }
}
