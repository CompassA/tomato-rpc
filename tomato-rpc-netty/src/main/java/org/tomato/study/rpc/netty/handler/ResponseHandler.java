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

package org.tomato.study.rpc.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.sender.ChannelResponseHolder;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Command> {

    public static final ResponseHandler INSTANCE = new ResponseHandler();

    private final ChannelResponseHolder responseHolder = ChannelResponseHolder.INSTANCE;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        long id = msg.getHeader().getId();
        responseHolder.remove(id).ifPresent(
                nettyResponse -> nettyResponse.getFuture().complete(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
}
