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

package org.tomato.study.rpc.netty.transport.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.serializer.Serializer;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.utils.Logger;

import java.util.Collections;

/**
 * 客户端心跳
 * @author Tomato
 * Created on 2021.10.05
 */
@ChannelHandler.Sharable
public class KeepAliveHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT) {
            // 客户端检测到空闲事件就发送一个心跳包
            ctx.writeAndFlush(
                    CommandFactory.request(
                            null,
                            SpiLoader.getLoader(Serializer.class).load(),
                            Collections.emptyMap(),
                            CommandType.KEEP_ALIVE_REQUEST)
            );
            Logger.DEFAULT.info("sent keep alive packet");
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
