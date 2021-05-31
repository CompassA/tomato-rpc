package org.tomato.study.rpc.netty.server.handler;

import io.netty.channel.ChannelHandler;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;
import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.netty.data.MethodContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@ChannelHandler.Sharable
public class RpcRequestHandler implements ProviderRegistry, ServerHandler {

    private final Map<String, Object> providerMap = new HashMap<>(0);

    @Override
    public synchronized <T> void register(String vip, T instance, Class<T> serviceInterface) {
        if (instance == null || !serviceInterface.isInterface()) {
            throw new IllegalCallerException("register invalid data");
        }
        providerMap.put(providerId(serviceInterface.getName(), vip), instance);
    }

    @Override
    public Command handle(Command command) throws Exception {
        Header header = command.getHeader();
        Serializer serializer = SerializerHolder.getSerializer(header.getSerializeType());
        MethodContext clientRequest = serializer.deserialize(command.getBody(), MethodContext.class);
        //todo Service VIP Config
        String providerId = providerId(clientRequest.getInterfaceName(), "mockVIP");
        Object providerInstance = providerMap.get(providerId);
        if (providerInstance == null) {
            throw new IllegalCallerException("no such provider named " + providerId);
        }
        Method method = providerInstance.getClass().getMethod(clientRequest.getMethodName(), clientRequest.getArgsType());
        Object result = method.invoke(providerInstance, clientRequest.getParameters());
        return CommandFactory.INSTANCE.createResponse(
                header.getId(), result, serializer, CommandType.RPC_RESPONSE);
    }

    @Override
    public CommandType getType() {
        return CommandType.RPC_REQUEST;
    }

    private String providerId(String interfaceName, String vip) {
        return interfaceName + "$" + vip;
    }
}
