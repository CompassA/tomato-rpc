package org.tomato.study.rpc.netty.sender;

import io.netty.channel.Channel;
import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class ChannelWrapper {

    private final Channel channel;

    public ChannelWrapper(Channel channel) {
        this.channel = channel;
    }
}
