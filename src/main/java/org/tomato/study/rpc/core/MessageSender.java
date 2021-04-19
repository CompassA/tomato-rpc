package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.protocol.Command;

import java.util.concurrent.CompletableFuture;

/**
 * send rpc request
 * @author Tomato
 * Created on 2021.03.31
 */
public interface MessageSender {

    /**
     * send rpc request
     * @param msg request message
     * @return response message
     */
    CompletableFuture<Command> send(Command msg);

    /**
     * get the vip of the sender target server
     * @return vip
     */
    String getServiceVIP();
}
