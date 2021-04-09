package org.tomato.study.rpc.impl.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.Header;

/**
 * @author Tomato
 * Created on 2021.04.09
 */
public class PacketEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          Command command,
                          ByteBuf byteBuf) throws Exception {
        Header header = command.getHeader();
        byteBuf.writeByte(header.getMagicNumber());
        byteBuf.writeInt(header.getVersion());
        byteBuf.writeInt(header.getHeaderLength());
        byteBuf.writeInt(header.getBodyLength());
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());
        byteBuf.writeBytes(command.getExtension());
        byteBuf.writeBytes(command.getBody());
    }
}
