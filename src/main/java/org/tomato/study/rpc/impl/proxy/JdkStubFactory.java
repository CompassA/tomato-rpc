package org.tomato.study.rpc.impl.proxy;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.SerializerHolder;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.CommandFactory;
import org.tomato.study.rpc.core.protocol.CommandType;
import org.tomato.study.rpc.data.MethodContext;

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
    public <T> T createStub(MessageSender messageSender, Class<T> serviceInterface) {
        InvocationHandler handler = new StubHandler(
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

        private final Class<?> serviceInterface;

        private final Serializer serializer;

        private final MessageSender messageSender;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                MethodContext methodContext = MethodContext.builder()
                        .interfaceName(serviceInterface.getName())
                        .methodName(method.getName())
                        .argsType(method.getParameterTypes())
                        .returnType(method.getReturnType())
                        .parameters(args)
                        .build();
                Command requestCommand = CommandFactory.INSTANCE.createRequest(
                        methodContext, serializer, CommandType.RPC_REQUEST);
                Command responseCommand = messageSender.send(requestCommand).get();
                return serializer.deserialize(
                        responseCommand.getBody(), methodContext.getReturnType());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
