package org.tomato.study.rpc.core;

import java.io.Closeable;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface RpcServer extends Closeable {

    /**
     * start rpc sever
     * @param handlerRegistry request handler which matches the request type by message type
     * @param port server port
     */
    void start(HandlerRegistry handlerRegistry, int port) throws InterruptedException;

    /**
     * get rpc server port
     * @return port
     */
    int getPort();

    /**
     * is rpc server closed
     * @return true: closed
     */
    boolean isClosed();
}
