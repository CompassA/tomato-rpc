package org.tomato.study.rpc.core;

import org.tomato.study.rpc.data.Command;

import java.util.concurrent.CompletableFuture;

/**
 * send rpc request
 * @author Tomato
 * Created on 2021.03.31
 */
public interface MsgSender {

    /**
     * send rpc request
     * @param msg request message
     * @return response message
     */
    CompletableFuture<Command> send(Command msg);
}
