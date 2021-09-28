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
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.data.RpcResponse;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcRequestHandler implements ServerHandler {

    /**
     * server provider map:
     * id generated by this#providerId -> provider instance
     */
    private final ProviderRegistry providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();

    @Override
    public Command handle(Command command) {
        Header header = command.getHeader();
        Serializer serializer = SerializerHolder.getSerializer(header.getSerializeType());
        try {
            // deserialize the RPC request
            RpcRequest request = serializer.deserialize(command.getBody(), RpcRequest.class);

            // get class interface
            String interfaceName = request.getInterfaceName();
            Class<?> providerInterface;
            try {
                providerInterface = Class.forName(interfaceName);
            } catch (ClassNotFoundException exception) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.NETTY_HANDLER_PROVIDER_NOT_FOUND.create(
                                "provider interface not found: " +interfaceName
                        ),
                        exception
                );
            }

            // search the RPC service provider
            Object provider = this.providerRegistry.getProvider(request.getServiceVIP(), providerInterface);
            if (provider == null) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.NETTY_HANDLER_PROVIDER_NOT_FOUND.create(
                                "provider not found: " + interfaceName
                        )
                );
            }

            // search the method of the provider
            Method method;
            try {
                method = providerInterface.getMethod(request.getMethodName(), request.getArgsType());
            } catch (NoSuchMethodException | SecurityException exception) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.NETTY_HANDLER_PROVIDER_NOT_FOUND.create(
                                "provider method not found: " + request.getMethodName()
                        ),
                        exception
                );
            }

            // invoke the method and write response data
            try {
                Object result = method.invoke(provider, request.getParameters());
                return CommandFactory.INSTANCE.response(
                        header.getId(),
                        RpcResponse.success(result),
                        serializer,
                        CommandType.RPC_RESPONSE);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.NETTY_HANDLER_RPC_INVOKER_ERROR.create(
                                "rpc method call failed: " + method.getName()
                        ),
                        exception
                );
            }
        } catch (TomatoRpcRuntimeException exception) {
            log.error(exception.getMessage(), exception);
            return CommandFactory.INSTANCE.response(
                    header.getId(),
                    RpcResponse.fail(exception),
                    serializer,
                    CommandType.RPC_REQUEST
            );

        }
    }

    @Override
    public CommandType getType() {
        return CommandType.RPC_REQUEST;
    }
}
