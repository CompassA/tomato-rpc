package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.CommandType;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public class HandlerRegistry {

    private final ConcurrentMap<CommandType, ServerHandler> providerMap = new ConcurrentHashMap<>(0);

    public HandlerRegistry() {
        for (ServerHandler serverHandler : SpiLoader.loadAll(ServerHandler.class)) {
            register(serverHandler);
        }
    }

    public void register(ServerHandler serverHandler) {
        providerMap.put(serverHandler.getType(), serverHandler);
    }

    public Optional<ServerHandler> match(CommandType type) {
        return Optional.ofNullable(providerMap.get(type));
    }
}
