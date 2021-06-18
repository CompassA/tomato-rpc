package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
@SpiInterface(paramName = "senderFactory")
public interface SenderFactory {

    /**
     * create message sender
     * @param serviceVIP service provider uri
     * @return messageSender
     */
    MessageSender create(String serviceVIP);
}
