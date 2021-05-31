package org.tomato.study.rpc.netty.sender;

import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.data.Command;

import java.util.concurrent.CompletableFuture;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class NettySender implements MessageSender {

    private final String serviceVIP;

    private final ChannelHolder channelHolder;

    private final ChannelResponseHolder responseHolder;

    public NettySender(String serviceVIP,
                       ChannelHolder channelHolder,
                       ChannelResponseHolder responseHolder) {
        this.serviceVIP = serviceVIP;
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        long id = msg.getHeader().getId();
        responseHolder.putFeatureResponse(id, future);
        try {
            channelHolder.getChannelWrapper(serviceVIP)
                    .getChannel()
                    .writeAndFlush(msg)
                    .addListener((ChannelFutureListener) futureChannel -> {
                        if (!futureChannel.isSuccess()) {
                            future.completeExceptionally(futureChannel.cause());
                            responseHolder.remove(id);
                        }
                    });
        } catch (Throwable e) {
            responseHolder.remove(id);
            future.completeExceptionally(e);
        }
        return future;
    }
}
