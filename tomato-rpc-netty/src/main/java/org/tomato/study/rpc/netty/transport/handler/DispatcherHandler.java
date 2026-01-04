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
import lombok.Getter;
import org.slf4j.MDC;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.CommandInterceptor;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.ExtensionHeader;
import org.tomato.study.rpc.core.data.ExtensionHeaderBuilder;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.RpcResponse;
import org.tomato.study.rpc.core.serializer.SerializerHolder;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.interceptor.CompressInterceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

/**
 * 请求分发器，根据请求中的CommandType将请求转发到对应的ServerHandler中
 * @author Tomato
 * Created on 2021.04.18
 */
@Getter
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<Command> {

    private final Map<CommandType, ServerHandler> handlerMap;
    private final CommandInterceptor[] interceptors;

    /**
     * 业务线程池, 可能为空
     */
    private final ExecutorService businessExecutor;

    public DispatcherHandler(ProviderRegistry providerRegistry, ExecutorService businessExecutor) {
        // 通过jdk spi加载依赖的ServerHandler
        ServiceLoader<ServerHandler> serverHandlers = ServiceLoader.load(ServerHandler.class);
        Map<CommandType, ServerHandler> serverHandlerMap = new HashMap<>(0);
        for (ServerHandler serverHandler : serverHandlers) {
            serverHandler.setProviderRegistry(providerRegistry);
            serverHandlerMap.put(serverHandler.getType(), serverHandler);
        }
        this.handlerMap = Collections.unmodifiableMap(serverHandlerMap);
        this.interceptors = new CommandInterceptor[] {
                new CompressInterceptor(),
        };
        this.businessExecutor = businessExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
        // 查找匹配的ServerHandler
        CommandType type = CommandType.value(msg.getHeader().getMessageType());
        ServerHandler matchHandler = handlerMap.get(type);
        if (matchHandler == null) {
            Logger.DEFAULT.warn("rpc server handler not found, type: {}", type);
            return;
        }
        // 如果是请求信息并且有业务线程池，交由业务线程池处理
        if (type == CommandType.RPC_REQUEST && businessExecutor != null) {
            businessExecutor.execute(() -> processRequest(ctx, msg, matchHandler));
            return;
        }
        // 处理具体逻辑
        processRequest(ctx, msg, matchHandler);
    }

    private void processRequest(ChannelHandlerContext ctx,
                                Command request,
                                ServerHandler matchHandler) {
        Map<String, String> extensionHeaders = ExtensionHeaderBuilder.getExtensionHeader(request);
        request = beforeProcess(request, extensionHeaders);
        Header header = request.getHeader();

        // 收到请求后, 将拓展参数塞入thread-local
        InvocationContext.set(extensionHeaders);
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        MDC.put(ExtensionHeader.TRACE_ID.name(), ExtensionHeader.TRACE_ID.getValueFromContext());
        try {
            Command response = matchHandler.handle(request);
            if (response == null) {
                return;
            }
            response = afterProcess(request, extensionHeaders, response);
            // 将结果写入缓存
            ctx.writeAndFlush(response).addListener(
                    // 若出现异常，log错误信息，给客户端返回RPC异常
                    (ChannelFutureListener) listener -> {
                        if (!listener.isSuccess()) {
                            Throwable cause = listener.cause();
                            Logger.DEFAULT.error(cause.getMessage(), cause);
                            Command errorResponse = CommandFactory.response(
                                    header.getId(),
                                    RpcResponse.fail(NettyRpcErrorEnum.NETTY_REQUEST_HANDLE_ERROR.create()),
                                    SerializerHolder.getSerializer(header.getSerializeType()),
                                    CommandType.RPC_RESPONSE);
                            ctx.writeAndFlush(errorResponse);
                            ctx.close();
                        }
                    });
        } catch (Throwable exception) {
            Logger.DEFAULT.error(exception.getMessage(), exception);
            ctx.writeAndFlush(
                    CommandFactory.response(
                            header.getId(),
                            RpcResponse.fail(NettyRpcErrorEnum.NETTY_REQUEST_HANDLE_ERROR.create()),
                            SerializerHolder.getSerializer(header.getSerializeType()),
                            CommandType.RPC_RESPONSE
                    )
            );
            ctx.close();
        } finally {
            // 请求处理完成, 清空ThreadLocal
            InvocationContext.remove();
            MDC.setContextMap(copyOfContextMap);
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

    protected Command afterProcess(Command request, Map<String, String> extensionHeaders, Command response) throws Exception {
        for (CommandInterceptor interceptor : interceptors) {
            response = interceptor.postProcessResponse(request, response, extensionHeaders);
        }
        return response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Logger.DEFAULT.error(cause.getMessage(), cause);
    }
}
