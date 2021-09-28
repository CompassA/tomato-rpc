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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.data.RpcResponse;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<Command> {

    private final ConcurrentMap<CommandType, ServerHandler> providerMap = new ConcurrentHashMap<>(0);

    public DispatcherHandler() {
        ServiceLoader<ServerHandler> serviceHandlers = ServiceLoader.load(ServerHandler.class);
        for (ServerHandler serviceHandler : serviceHandlers) {
            this.register(serviceHandler);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        try {
            CommandType type = CommandType.value(msg.getHeader().getMessageType());
            Optional<ServerHandler> matchHandler = match(type);
            if (matchHandler.isEmpty()) {
                throw new IllegalStateException("rpc server handler not found, type: " + type);
            }
            Command handleResult = matchHandler.get().handle(msg);
            ctx.channel().writeAndFlush(handleResult).addListener(
                    (ChannelFutureListener) listener -> {
                        if (!listener.isSuccess()) {
                            listener.cause().printStackTrace();
                            ctx.channel().writeAndFlush(
                                    CommandFactory.INSTANCE.response(
                                            msg.getHeader().getId(),
                                            RpcResponse.fail(new TomatoRpcRuntimeException(
                                                    NettyRpcErrorEnum.NETTY_HANDLER_WRITE_ERROR.create())),
                                            SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                                            CommandType.RPC_RESPONSE
                                    )
                            );
                            ctx.channel().close();
                        }
                    });
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            ctx.channel().writeAndFlush(
                    CommandFactory.INSTANCE.response(
                            msg.getHeader().getId(),
                            RpcResponse.fail(new TomatoRpcRuntimeException(
                                    NettyRpcErrorEnum.NETTY_HANDLER_WRITE_ERROR.create())),
                            SerializerHolder.getSerializer(msg.getHeader().getSerializeType()),
                            CommandType.RPC_RESPONSE)
            );
            ctx.channel().close();
        }
    }

    private Optional<ServerHandler> match(CommandType type) {
        return Optional.ofNullable(this.providerMap.get(type));
    }

    private void register(ServerHandler serverHandler) {
        this.providerMap.put(serverHandler.getType(), serverHandler);
    }
}
