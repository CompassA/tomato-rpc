package org.tomato.study.rpc.impl.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.tomato.study.rpc.core.protocol.Command;

/**
 * @author Tomato
 * Created on 2021.04.09
 */
public class RawFrameEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Command command,
                          ByteBuf byteBuf) throws Exception {
        Command.encode(command, byteBuf);
    }
}
