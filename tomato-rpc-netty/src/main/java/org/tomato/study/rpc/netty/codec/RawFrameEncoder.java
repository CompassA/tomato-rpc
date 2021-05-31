package org.tomato.study.rpc.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.utils.CommandUtil;

/**
 * @author Tomato
 * Created on 2021.04.09
 */
public class RawFrameEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Command command,
                          ByteBuf byteBuf) throws Exception {
        CommandUtil.encode(command, byteBuf);
    }
}
