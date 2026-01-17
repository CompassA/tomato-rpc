/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty.codec;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.ProtoConstants;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;

/**
 * 对基于Netty实现的RPC数据帧进行编解码
 * @author Tomato
 * Created on 2021.05.30
 */
public final class NettyCommandCodec {

    private NettyCommandCodec() throws IllegalAccessException {
        throw new IllegalAccessException("illegal access");
    }

    public static void encode(Command command, ByteBuf byteBuf) {
        Header header = command.getHeader();
        int extensionLength = header.getExtensionLength();
        int length = header.getLength();

        byteBuf.writeByte(header.getMagicNumber());
        byteBuf.writeInt(length);
        byteBuf.writeInt(header.getVersion());
        byteBuf.writeInt(extensionLength);
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());

        // 不为空时才写入拓展字段
        byte[] extension = command.getExtension();
        if (extensionLength > 0 && extension != null) {
            if (extension.length != extensionLength) {
                throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.CODEC_ENCODE_ERROR, "frame extension length error");
            }
            byteBuf.writeBytes(extension);
        }

        // 不为空时才写入body体
        byte[] body = command.getBody();
        if (length > 0 && body != null) {
            int bodyLength = length - extensionLength - ProtoConstants.HEAD_FIX_LENGTH;
            if (bodyLength != body.length) {
                throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.CODEC_ENCODE_ERROR, "frame body length error");
            }
            byteBuf.writeBytes(body);
        }
    }

    @NonNull
    @SuppressWarnings("uncheck")
    public static Command decode(ByteBuf byteBuf) {
        Command.CommandBuilder commandBuilder = Command.builder();

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
        commandBuilder.header(header);

        // read extension
        int extensionLength = header.getExtensionLength();
        if (extensionLength > 0) {
            byte[] extension = new byte[extensionLength];
            byteBuf.readBytes(extension);
            commandBuilder.extension(extension);
        }

        // read body
        int bodyLength = header.getLength() - extensionLength - ProtoConstants.HEAD_FIX_LENGTH;
        if (bodyLength > 0) {
            byte[] body = new byte[bodyLength];
            byteBuf.readBytes(body);
            commandBuilder.body(body);
        }

        return commandBuilder.build();
    }
}
