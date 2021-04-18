package org.tomato.study.rpc.impl.sender.netty;

import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.protocol.Command;

import java.util.concurrent.CompletableFuture;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class NettySender implements MessageSender {

    private final String serviceVip;

    private final ChannelHolder channelHolder;

    private final ChannelResponseHolder responseHolder;

    public NettySender(String serviceVip,
                       ChannelHolder channelHolder,
                       ChannelResponseHolder responseHolder) {
        this.serviceVip = serviceVip;
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        long id = msg.getHeader().getId();
        responseHolder.putFeatureResponse(id, future);
        try {
            channelHolder.getChannelWrapper(serviceVip)
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
