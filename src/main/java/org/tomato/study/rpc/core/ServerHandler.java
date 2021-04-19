package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.CommandType;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface ServerHandler {

    /**
     * handle request
     * @param command request
     * @return response
     * @throws Exception any exception during server handle
     */

    Command handle(Command command) throws Exception;

    /**
     * request which command
     * @return target command type
     */
    CommandType getType();
}
