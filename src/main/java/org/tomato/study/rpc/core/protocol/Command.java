package org.tomato.study.rpc.core.protocol;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * rpc protocol frame
 * every rpc request/response will send a command to server/client
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    /**
     * frame header, fix length
     */
    private Header header;

    /**
     * frame extension header, dynamic length
     */
    private byte[] extension;

    /**
     * frame payload data which has been serialized
     */
    private byte[] body;

    public static void encode(Command command, ByteBuf byteBuf) {
        Header header = command.getHeader();
        byteBuf.writeByte(header.getMagicNumber());
        byteBuf.writeInt(header.getLength());
        byteBuf.writeInt(header.getVersion());
        byteBuf.writeInt(header.getExtensionLength());
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());
        byteBuf.writeBytes(command.extension);
        byteBuf.writeBytes(command.body);
    }

    @SuppressWarnings("uncheck")
    public static Command decode(ByteBuf byteBuf) {
        // reader header
        Header header = Header.builder()
                .magicNumber(byteBuf.readByte())
                .length(byteBuf.readInt())
                .version(byteBuf.readInt())
                .extensionLength(byteBuf.readInt())
                .messageType(byteBuf.readShort())
                .serializeType(byteBuf.readByte())
                .id(byteBuf.readLong())
                .build();

        // read extension
        byte[] extension;
        int extensionLength = header.getExtensionLength();
        if (extensionLength > 0) {
            extension = new byte[extensionLength];
            byteBuf.readBytes(extension);
        } else {
            extension = new byte[0];
        }

        // read body
        int bodyLength = header.getLength() - extensionLength - ProtoConstants.HEAD_FIX_LENGTH;
        byte[] body = new byte[bodyLength];
        byteBuf.readBytes(body);

        return Command.builder()
                .header(header)
                .extension(extension)
                .body(body)
                .build();
    }
}
