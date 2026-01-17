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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.tomato.study.rpc.common.utils.Logger;

import java.util.concurrent.TimeUnit;

/**
 * 服务端空闲检测
 * @author Tomato
 * Created on 2021.10.03
 */
public class ServerIdleCheckHandler extends IdleStateHandler {

    /**
     * 连接在配置的时间段内无读操作时，触发空闲检测
     * @param readerIdleTime 配置的空闲时间段
     */
    public ServerIdleCheckHandler(long readerIdleTime) {
        super(readerIdleTime, 0, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        // 检测到空闲后关闭连接
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            ctx.close();
            Logger.DEFAULT.info("server idle check, close a remote channel");
            return;
        }
        super.channelIdle(ctx, evt);
    }
}
