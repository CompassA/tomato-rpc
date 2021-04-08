package org.tomato.study.rpc.impl.sender.netty;

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

    private ChannelManager channelManager;

    public NettySender(String serviceVip) {
        this.serviceVip = serviceVip;
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        return null;
    }
}
