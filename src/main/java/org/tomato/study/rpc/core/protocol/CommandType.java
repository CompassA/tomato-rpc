package org.tomato.study.rpc.core.protocol;

import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
@Getter
public enum CommandType {

    /**
     * RPC client request
     */
    RPC_REQUEST((short) 1),

    /**
     * RPC server response
     */
    RPC_RESPONSE((short) 2),
    ;

    private final short id;

    CommandType(short id) {
        this.id = id;
    }

}
