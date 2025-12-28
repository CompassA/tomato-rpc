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
import lombok.RequiredArgsConstructor;
import org.tomato.study.rpc.core.CommandInterceptor;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.ExtensionHeaderBuilder;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.netty.interceptor.CompressInterceptor;
import org.tomato.study.rpc.netty.transport.client.NettyResponseHolder;
import org.tomato.study.rpc.utils.Logger;

import java.util.Map;

/**
 * 处理RPC服务端的响应数据
 * @author Tomato
 * Created on 2021.04.17
 */
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class ResponseHandler extends SimpleChannelInboundHandler<Command> {

    private final NettyResponseHolder responseHolder;
    private final CommandInterceptor[] interceptors = new CommandInterceptor[] {
            new CompressInterceptor(),
    };

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command request) throws Exception {
        Map<String, String> extensionHeaders = ExtensionHeaderBuilder.getExtensionHeader(request);
        request = beforeProcess(request, extensionHeaders);
        Header header = request.getHeader();
        CommandType type = CommandType.value(header.getMessageType());
        switch (type) {
            case RPC_RESPONSE:
                handleRpcResponse(request, header);
                break;
            case KEEP_ALIVE_RESPONSE:
                handleKeepAliveResponse(request, header);
                break;
            default:
                Logger.DEFAULT.warn("received unknown command type: {}", type);
        }
    }

    protected Command beforeProcess(Command request, Map<String, String> extensionHeaders) {
        for (CommandInterceptor interceptor : interceptors) {
            try {
                request = interceptor.interceptRequest(request, extensionHeaders);
            } catch (Exception e) {
                Logger.DEFAULT.error("intercept request error", e);
            }
        }
        return request;
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
        Logger.DEFAULT.info("keep alive response received");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Logger.DEFAULT.error(cause.getMessage(), cause);
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }
}
