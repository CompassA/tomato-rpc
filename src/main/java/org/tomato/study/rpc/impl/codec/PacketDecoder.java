package org.tomato.study.rpc.impl.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.Header;
import org.tomato.study.rpc.core.protocol.ProtoConstants;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.09
 */
public class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          ByteBuf byteBuf,
                          List<Object> list) throws Exception {
        byteBuf.markReaderIndex();
        if (!byteBuf.isReadable(ProtoConstants.HEAD_FIX_LENGTH) ||
                byteBuf.readByte() != ProtoConstants.MAGIC_NUMBER) {
            return;
        }
        Header header = Header.builder()
                .magicNumber(ProtoConstants.MAGIC_NUMBER)
                .version(byteBuf.readInt())
                .headerLength(byteBuf.readInt())
                .bodyLength(byteBuf.readInt())
                .messageType(byteBuf.readShort())
                .serializeType(byteBuf.readByte())
                .id(byteBuf.readLong())
                .build();

        int extensionLength = header.getHeaderLength() - ProtoConstants.HEAD_FIX_LENGTH;
        if (!byteBuf.isReadable(header.getBodyLength() + extensionLength)) {
            byteBuf.resetReaderIndex();
            return;
        }

        byte[] extension;
        if (extensionLength > 0) {
            extension = new byte[extensionLength];
            byteBuf.readBytes(extension);
        } else {
            extension = new byte[0];
        }

        byte[] body = new byte[header.getBodyLength()];
        byteBuf.readBytes(body);

        list.add(Command.builder().header(header).extension(extension).body(body).build());
    }
}
