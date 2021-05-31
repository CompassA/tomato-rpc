package org.tomato.study.rpc.netty.sender;

import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.SenderFactory;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public final class NettySenderFactory implements SenderFactory {

    @Override
    public MessageSender create(String serviceVIP) {
        return new NettySender(serviceVIP,
                ChannelHolder.INSTANCE, ChannelResponseHolder.INSTANCE);
    }
}
