package org.tomato.study.rpc.impl.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.Header;
import org.tomato.study.rpc.impl.TestCommonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.10
 */
public class CodecTest {

    private Command mockCommand;

    private final RawFrameDecoder rawFrameDecoder = new RawFrameDecoder();

    private final RawFrameEncoder rawFrameEncoder = new RawFrameEncoder();

    @Before
    public void init() {
        this.mockCommand = TestCommonUtil.mockCommand();
    }

    @Test
    public void encodeDecodeTest() throws Exception {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();

        rawFrameEncoder.encode(null, mockCommand, byteBuf);

        List<Object> res = new ArrayList<>(1);
        rawFrameDecoder.decode(null, byteBuf, res);
        Command decodeResult = (Command) res.get(0);

        Header mockHeader = mockCommand.getHeader();
        Header decodeResultHeader = decodeResult.getHeader();
        Assert.assertEquals(mockHeader.getMagicNumber(), decodeResultHeader.getMagicNumber());
        Assert.assertEquals(mockHeader.getVersion(), decodeResultHeader.getVersion());
        Assert.assertEquals(mockHeader.getExtensionLength(), decodeResultHeader.getExtensionLength());
        Assert.assertEquals(mockHeader.getLength(), decodeResultHeader.getLength());
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
        byteBuf.writeInt(header.getExtensionLength());
        byteBuf.writeInt(header.getLength());
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());
        rawFrameDecoder.decode(null, byteBuf, null);
        Assert.assertEquals(0, byteBuf.readerIndex());
    }
}
