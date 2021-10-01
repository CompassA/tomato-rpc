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
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.sender.NettyResponseHolder;

/**
 * 处理RPC服务端的响应数据
 * @author Tomato
 * Created on 2021.04.17
 */
@Slf4j
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Command> {

    @Deprecated
    public static final ResponseHandler INSTANCE = new ResponseHandler(null);

    /**
     * RPC客户端所有的ResponseFuture
     */
    private final NettyResponseHolder responseHolder;

    public ResponseHandler(NettyResponseHolder responseHolder) {
        this.responseHolder = responseHolder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        if (msg.getHeader() == null) {
            throw new IllegalStateException("no header data");
        }

        // 根据消息id拿到对应的future
        long id = msg.getHeader().getId();
        try {
            // 将结果注入future使客户端停止等待
            responseHolder.getAndRemove(id)
                    .ifPresent(nettyResponse ->
                            nettyResponse.getFuture().complete(msg));
        } catch (RuntimeException exception) {
            responseHolder.getAndRemove(id)
                    .ifPresent(nettyResponse ->
                            nettyResponse.getFuture()
                                    .completeExceptionally(exception));
            throw exception;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
}
