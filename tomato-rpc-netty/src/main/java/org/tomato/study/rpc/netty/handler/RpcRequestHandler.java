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
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.data.RpcRequestDTO;
import org.tomato.study.rpc.netty.data.RpcRequestModel;
import org.tomato.study.rpc.netty.data.RpcResponse;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;
import org.tomato.study.rpc.netty.utils.ConvertUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 服务端处理RPC请求的Handler
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcRequestHandler implements ServerHandler {

    /**
     * 服务端RPC服务实现类对象的管理器
     */
    private final ProviderRegistry providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();

    @Override
    public Command handle(Command command) {
        Header header = command.getHeader();
        Serializer serializer = SerializerHolder.getSerializer(command.getHeader().getSerializeType());
        try {
            // 根据请求中的序列化算法类型，进行反序列化
            RpcRequestModel request = getRpcRequestModel(command, serializer);

            // 得到接口
            Class<?> providerInterface = request.getRpcInterFace();

            // 根据客户端提供的目标vip值和目标接口查找服务端的实现类
            Object provider = providerRegistry.getProvider(request.getServiceVIP(), providerInterface);

            // 查找目标方法
            Method method = searchMethod(request, providerInterface, provider);

            // 反射调用
            return invoke(header, serializer, request, provider, method);

        } catch (TomatoRpcException exception) {
            log.error(exception.getMessage(), exception);
            return CommandFactory.response(
                    header.getId(),
                    RpcResponse.fail(exception.getErrorInfo()),
                    serializer,
                    CommandType.RPC_REQUEST
            );
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.RPC_REQUEST;
    }

    private Method searchMethod(RpcRequestModel request,
                                Class<?> providerInterface,
                                Object provider) throws TomatoRpcException {
        if (provider == null) {
            throw new TomatoRpcException(
                    NettyRpcErrorEnum.NETTY_HANDLER_PROVIDER_NOT_FOUND.create(
                            "provider not found: " + providerInterface.getName()));
        }

        try {
            return providerInterface.getMethod(request.getMethodName(), request.getArgsType());
        } catch (NoSuchMethodException | SecurityException exception) {
            throw new TomatoRpcException(
                    NettyRpcErrorEnum.NETTY_HANDLER_PROVIDER_NOT_FOUND.create(
                            "provider method not found: " + request.getMethodName()),
                    exception
            );
        }
    }

    private RpcRequestModel getRpcRequestModel(Command command, Serializer serializer) throws TomatoRpcException {
        try {
            return ConvertUtils.convert(
                    serializer.deserialize(command.getBody(), RpcRequestDTO.class)
            );
        } catch (ClassNotFoundException e) {
            throw new TomatoRpcException(
                    NettyRpcErrorEnum.MODEL_DTO_CONVERT_ERROR.create());
        }
    }

    private Command invoke(Header header,
                           Serializer serializer,
                           RpcRequestModel request,
                           Object provider,
                           Method method) throws TomatoRpcException {
        try {
            Object result = method.invoke(provider, request.getParameters());
            return CommandFactory.response(
                    header.getId(),
                    RpcResponse.success(result),
                    serializer,
                    CommandType.RPC_RESPONSE);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            throw new TomatoRpcException(
                    NettyRpcErrorEnum.NETTY_HANDLER_RPC_INVOKER_ERROR.create(
                            "rpc method call failed: " + method.getName()),
                    exception
            );
        }
    }
}
