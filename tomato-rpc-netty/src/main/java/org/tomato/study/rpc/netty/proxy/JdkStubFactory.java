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

package org.tomato.study.rpc.netty.proxy;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.data.Code;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.data.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public class JdkStubFactory implements StubFactory {

    private final Serializer serializer = SpiLoader.getLoader(Serializer.class).load();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(MessageSender messageSender, Class<T> serviceInterface, String serviceVIP) {
        InvocationHandler handler = new StubHandler(
                serviceVIP,
                serviceInterface,
                serializer,
                messageSender);
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[] { serviceInterface },
                handler);
    }

    @AllArgsConstructor
    private static class StubHandler implements InvocationHandler {

        private final String serviceVIP;

        private final Class<?> serviceInterface;

        private final Serializer serializer;

        private final MessageSender messageSender;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                RpcRequest request = RpcRequest.builder()
                        .serviceVIP(serviceVIP)
                        .interfaceName(serviceInterface.getName())
                        .methodName(method.getName())
                        .argsType(method.getParameterTypes())
                        .returnType(method.getReturnType())
                        .parameters(args)
                        .build();
                Command requestCommand = CommandFactory.INSTANCE.request(
                        request, serializer, CommandType.RPC_REQUEST);
                Command responseCommand = messageSender.send(requestCommand).get();
                RpcResponse response = serializer.deserialize(
                        responseCommand.getBody(), RpcResponse.class);
                if (Code.SUCCESS.equals(response.getCode())) {
                    return response.getData();
                } else {
                    throw new TomatoRpcException("rpc failed, server message: " + response.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
