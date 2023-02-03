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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.ProtoConstants;

import java.util.List;

/**
 * 二进制字节 ---解码---> {@link org.tomato.study.rpc.core.data.Command}
 * @author Tomato
 * Created on 2021.04.16
 */
@Slf4j
public class NettyProtoDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf byteBuf,
                          List<Object> list) throws Exception {
        Command command = NettyCommandCodec.decode(byteBuf);
        Header header = command.getHeader();
        if (header == null) {
            throw new IllegalStateException("command header is null");
        }
        if (ProtoConstants.MAGIC_NUMBER != header.getMagicNumber()) {
            throw new IllegalStateException("magic number is not valid");
        }
        if (header.getLength() < 0 || header.getExtensionLength() < 0) {
            throw new IllegalStateException("illegal length");
        }
        list.add(command);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("decode command error", cause);
        ctx.close();
    }
}
