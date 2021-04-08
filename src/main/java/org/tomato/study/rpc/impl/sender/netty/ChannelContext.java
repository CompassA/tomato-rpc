package org.tomato.study.rpc.impl.sender.netty;

import io.netty.channel.Channel;
import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class ChannelContext {

    private final Channel channel;

    private final ChannelResponseHolder responseHolder;

    public ChannelContext(Channel channel) {
        this.channel = channel;
        this.responseHolder = new ChannelResponseHolder();
    }
}
