package org.tomato.study.rpc.impl.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.Header;
import org.tomato.study.rpc.core.protocol.ProtoConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.10
 */
public class CodecTest {

    private Command mockCommand;

    private PacketDecoder packetDecoder = new PacketDecoder();

    private PacketEncoder packetEncoder = new PacketEncoder();

    @Before
    public void init() {
        byte[] extension = new byte[] {2, 3, 12, 45, 45, 34, 5 };
        byte[] body = new byte[] {1, 2, 3, 4, 5, 6, 6, 6, 23, 123, 12, 31};

        this.mockCommand = Command.builder()
                .header(Header.builder()
                        .magicNumber(ProtoConstants.MAGIC_NUMBER)
                        .version(ProtoConstants.CURRENT_VERSION)
                        .headerLength(extension.length + ProtoConstants.HEAD_FIX_LENGTH)
                        .bodyLength(body.length)
                        .messageType((short) 1)
                        .serializeType((byte) 2)
                        .id(12)
                        .build())
                .extension(extension)
                .body(body)
                .build();
    }

    @Test
    public void encodeDecodeTest() throws Exception {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();

        packetEncoder.encode(null, mockCommand, byteBuf);

        List<Object> res = new ArrayList<>(1);
        packetDecoder.decode(null, byteBuf, res);
        Command decodeResult = (Command) res.get(0);

        Header mockHeader = mockCommand.getHeader();
        Header decodeResultHeader = decodeResult.getHeader();
        Assert.assertEquals(mockHeader.getMagicNumber(), decodeResultHeader.getMagicNumber());
        Assert.assertEquals(mockHeader.getVersion(), decodeResultHeader.getVersion());
        Assert.assertEquals(mockHeader.getHeaderLength(), decodeResultHeader.getHeaderLength());
        Assert.assertEquals(mockHeader.getBodyLength(), decodeResultHeader.getBodyLength());
        Assert.assertEquals(mockHeader.getId(), decodeResultHeader.getId());
        Assert.assertEquals(mockHeader.getSerializeType(), decodeResultHeader.getSerializeType());
        Assert.assertEquals(mockHeader.getMessageType(), decodeResultHeader.getMessageType());
        Assert.assertArrayEquals(mockCommand.getBody(), decodeResult.getBody());
        Assert.assertArrayEquals(mockCommand.getExtension(), decodeResult.getExtension());
    }

    @Test
    public void unpreparedTest() throws Exception {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        Header header = mockCommand.getHeader();
        byteBuf.writeByte(header.getMagicNumber());
        byteBuf.writeInt(header.getVersion());
        byteBuf.writeInt(header.getHeaderLength());
        byteBuf.writeInt(header.getBodyLength());
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());
        packetDecoder.decode(null, byteBuf, null);
        Assert.assertEquals(0, byteBuf.readerIndex());
    }
}
