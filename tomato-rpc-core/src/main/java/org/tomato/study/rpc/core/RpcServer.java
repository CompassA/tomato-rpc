package org.tomato.study.rpc.core;

import java.io.Closeable;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface RpcServer extends Closeable {

    /**
     * start rpc sever
     */
    void start() throws InterruptedException;


    /**
     * get rpc server ip
     * @return local host
     */
    String getHost();

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
