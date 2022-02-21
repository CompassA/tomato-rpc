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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.netty.data.RpcResponse;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * 请求分发器，根据请求中的CommandType将请求转发到对应的ServerHandler中
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<Command> {

    private final ConcurrentMap<CommandType, ServerHandler> providerMap = new ConcurrentHashMap<>(0);

    @Setter
    private ExecutorService businessExecutor;

    public DispatcherHandler() {
        // 通过jdk spi加在依赖的ServerHandler
        ServiceLoader<ServerHandler> serverHandlers = ServiceLoader.load(ServerHandler.class);
        for (ServerHandler serverHandler : serverHandlers) {
            providerMap.put(serverHandler.getType(), serverHandler);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        // 查找匹配的ServerHandler
        CommandType type = CommandType.value(msg.getHeader().getMessageType());
        ServerHandler matchHandler = providerMap.get(type);
        if (matchHandler == null) {
            log.warn("rpc server handler not found, type: " + type);
            return;
        }
        // 如果是请求信息并且有业务线程池，交由业务线程池处理
        if (type == CommandType.RPC_REQUEST && businessExecutor != null) {
            businessExecutor.execute(() -> processRequest(ctx, msg, matchHandler));
        }
        // 处理具体逻辑
        processRequest(ctx, msg, matchHandler);
    }

    private void processRequest(ChannelHandlerContext ctx, Command msg, ServerHandler matchHandler) {
        try {
            Command handleResult = matchHandler.handle(msg);
            if (handleResult == null) {
                return;
            }

            // 将结果写入缓存
            ctx.writeAndFlush(handleResult).addListener(
                    // 若出现异常，log错误信息，给客户端返回RPC异常
                    (ChannelFutureListener) listener -> {
                        if (!listener.isSuccess()) {
                            Throwable cause = listener.cause();
                            log.error(cause.getMessage(), cause);
                            Command errorResponse = CommandFactory.response(
                                    msg.getHeader().getId(),
                                    RpcResponse.fail(NettyRpcErrorEnum.NETTY_REQUEST_HANDLE_ERROR.create()),
                                    SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                                    CommandType.RPC_RESPONSE);
                            ctx.writeAndFlush(errorResponse);
                            ctx.close();
                        }
                    });
        } catch (Throwable exception) {
            log.error(exception.getMessage(), exception);
            ctx.writeAndFlush(
                    CommandFactory.response(
                            msg.getHeader().getId(),
                            RpcResponse.fail(NettyRpcErrorEnum.NETTY_REQUEST_HANDLE_ERROR.create()),
                            SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                            CommandType.RPC_RESPONSE
                    )
            );
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
    }
}
