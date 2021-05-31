package org.tomato.study.rpc.core.data;

import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
@Getter
public enum CommandType {

    /**
     * unknown command type
     */
    UNKNOWN((short) 0),

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

    public static CommandType value(final Short id) {
        if (id == null) {
            return CommandType.UNKNOWN;
        }
        for (CommandType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return CommandType.UNKNOWN;
    }

}
