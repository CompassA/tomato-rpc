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

package org.tomato.study.rpc.netty.utils;

import io.netty.buffer.ByteBuf;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandModel;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.Parameter;
import org.tomato.study.rpc.core.data.ProtoConstants;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.ArrayList;

/**
 * @author Tomato
 * Created on 2021.05.30
 */
public final class CommandUtil {

    private CommandUtil() {
        throw new IllegalStateException("illegal access");
    }

    public static void encode(Command command, ByteBuf byteBuf) {
        Header header = command.getHeader();
        byteBuf.writeByte(header.getMagicNumber());
        byteBuf.writeInt(header.getLength());
        byteBuf.writeInt(header.getVersion());
        byteBuf.writeInt(header.getExtensionLength());
        byteBuf.writeShort(header.getMessageType());
        byteBuf.writeByte(header.getSerializeType());
        byteBuf.writeLong(header.getId());
        byteBuf.writeBytes(header.getExtensionLength() == 0 ?
                new byte[0] : command.getExtension());
        byteBuf.writeBytes(command.getBody());
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

    public static <T> CommandModel<T> toModel(Command command, Class<T> bodyType) {
        Header header = command.getHeader();
        Serializer serializer = SerializerHolder.getSerializer(header.getSerializeType());
        return CommandModel.<T>builder()
                .header(header)
                .extension(command.getExtension() == null || command.getExtension().length < 1
                        ? new ArrayList<>(0)
                        : serializer.deserializeList(command.getExtension(), Parameter.class))
                .body(serializer.deserialize(command.getBody(), bodyType))
                .build();
    }
}
