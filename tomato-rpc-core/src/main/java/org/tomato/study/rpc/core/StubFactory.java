package org.tomato.study.rpc.core;

/**
 * create rpc client proxy
 * @author Tomato
 * Created on 2021.03.31
 */
public interface StubFactory {

    /**
     * create a proxy instance which can send message to provider
     * @param messageSender the instance to send message to server
     * @param serviceInterface interface of service provider
     * @param serviceVIP service vip
     * @param <T> proxy interface type
     * @return proxy instance
     */
    <T> T createStub(MessageSender messageSender, Class<T> serviceInterface, String serviceVIP);
}
