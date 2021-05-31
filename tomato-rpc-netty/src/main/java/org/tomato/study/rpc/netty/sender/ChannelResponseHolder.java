package org.tomato.study.rpc.netty.sender;

import lombok.NoArgsConstructor;
import org.tomato.study.rpc.core.data.Command;

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

    private final ConcurrentMap<Long, NettyResponseFuture> responseMap = new ConcurrentHashMap<>(0);

    public void putFeatureResponse(long id, CompletableFuture<Command> future) {
        responseMap.put(id, new NettyResponseFuture(id, future, System.nanoTime()));
    }

    public Optional<NettyResponseFuture> getResponse(long id) {
        return Optional.ofNullable(responseMap.get(id));
    }

    public Optional<NettyResponseFuture> remove(long id) {
        return Optional.ofNullable(responseMap.remove(id));
    }
}
