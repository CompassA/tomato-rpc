package org.tomato.study.rpc.netty.proxy;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public class JdkStubFactory implements StubFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(MessageSender messageSender, Class<T> serviceInterface, String serviceVIP) {
        InvocationHandler handler = new StubHandler(
                serviceVIP,
                serviceInterface,
                SerializerHolder.getSerializer((byte) 0),
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
                RpcRequest rpcRequest = RpcRequest.builder()
                        .serviceVIP(serviceVIP)
                        .interfaceName(serviceInterface.getName())
                        .methodName(method.getName())
                        .argsType(method.getParameterTypes())
                        .returnType(method.getReturnType())
                        .parameters(args)
                        .build();
                Command requestCommand = CommandFactory.INSTANCE.requestCommand(
                        rpcRequest, serializer, CommandType.RPC_REQUEST);
                Command responseCommand = messageSender.send(requestCommand).get();
                return serializer.deserialize(
                        responseCommand.getBody(), rpcRequest.getReturnType());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
