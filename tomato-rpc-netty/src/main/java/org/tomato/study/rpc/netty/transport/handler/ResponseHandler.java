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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.netty.transport.client.NettyResponseHolder;

/**
 * 处理RPC服务端的响应数据
 * @author Tomato
 * Created on 2021.04.17
 */
@Slf4j
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Command> {

    /**
     * RPC客户端所有的ResponseFuture
     */
    private final NettyResponseHolder responseHolder;

    public ResponseHandler(NettyResponseHolder responseHolder) {
        this.responseHolder = responseHolder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        Header header = msg.getHeader();
        if (header == null) {
            throw new IllegalStateException("no header data");
        }
        CommandType type = CommandType.value(header.getMessageType());
        switch (type) {
            case RPC_RESPONSE:
                handleRpcResponse(msg, header);
                break;
            case KEEP_ALIVE_RESPONSE:
                handleKeepAliveResponse(msg, header);
                break;
            default:
                log.warn("received unknown command type: {}", type);
        }



    }

    private void handleRpcResponse(Command msg, Header header) {
        // 根据消息id拿到对应的future
        long id = header.getId();
        try {
            // 将结果注入future使客户端停止等待
            responseHolder.getAndRemove(id)
                    .ifPresent(nettyResponse -> nettyResponse.complete(msg));
        } catch (Throwable exception) {
            // 出现异常注入异常使客户端停止等待
            responseHolder.getAndRemove(id)
                    .ifPresent(nettyResponse -> nettyResponse.completeExceptionally(exception));
            throw exception;
        }
    }

    private void handleKeepAliveResponse(Command msg, Header header) {
        // do nothing
        log.info("keep alive response received");
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
