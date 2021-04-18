package org.tomato.study.rpc.impl.sender.netty;

import lombok.NoArgsConstructor;
import org.tomato.study.rpc.core.protocol.Command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@NoArgsConstructor
public class ChannelResponseHolder {

    public static final ChannelResponseHolder INSTANCE = new ChannelResponseHolder();

    private final ConcurrentMap<Long, NettyResponse> responseMap = new ConcurrentHashMap<>(0);

    public void putFeatureResponse(long id, CompletableFuture<Command> future) {
        responseMap.put(id, new NettyResponse(id, future, System.nanoTime()));
    }

    public Optional<NettyResponse> getResponse(long id) {
        return Optional.ofNullable(responseMap.get(id));
    }

    public Optional<NettyResponse> remove(long id) {
        return Optional.ofNullable(responseMap.remove(id));
    }
}
