/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.netty.TestCommonUtil;

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
        Assert.assertEquals(decodeResult, mockCommand);
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
