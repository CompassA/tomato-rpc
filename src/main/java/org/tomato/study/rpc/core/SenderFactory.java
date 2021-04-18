package org.tomato.study.rpc.core;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public interface SenderFactory {

    /**
     * create message sender
     * @param serviceVIP service provider uri
     * @return messageSender
     */
    MessageSender create(String serviceVIP);
}
