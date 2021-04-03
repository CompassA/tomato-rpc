package org.tomato.study.rpc.core;

/**
 * create rpc client proxy
 * @author Tomato
 * Created on 2021.03.31
 */
public interface StubFactory {

    /**
     * create a proxy instance which can send message to provider
     * @param msgSender the instance to send message to server
     * @param serviceInterface interface of service provider
     * @param <T> proxy interface type
     * @return proxy instance
     */
    <T> T createStub(MsgSender msgSender, Class<T> serviceInterface);
}
