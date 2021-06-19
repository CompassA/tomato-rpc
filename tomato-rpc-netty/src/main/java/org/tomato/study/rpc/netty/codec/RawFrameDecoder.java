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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.ProtoConstants;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.09
 */
public class RawFrameDecoder extends ByteToMessageDecoder {

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
                .length(byteBuf.readInt())
                .version(byteBuf.readInt())
                .extensionLength(byteBuf.readInt())
                .messageType(byteBuf.readShort())
                .serializeType(byteBuf.readByte())
                .id(byteBuf.readLong())
                .build();

        if (!byteBuf.isReadable(header.getLength() - ProtoConstants.HEAD_FIX_LENGTH)) {
            byteBuf.resetReaderIndex();
            return;
        }

        int extensionLength = header.getExtensionLength();
        byte[] extension;
        if (extensionLength > 0) {
            extension = new byte[extensionLength];
            byteBuf.readBytes(extension);
        } else {
            extension = new byte[0];
        }

        byte[] body = new byte[header.getLength() - header.getExtensionLength() - ProtoConstants.HEAD_FIX_LENGTH];
        byteBuf.readBytes(body);

        list.add(Command.builder().header(header).extension(extension).body(body).build());
    }
}
