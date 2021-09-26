package org.tomato.study.rpc.core.error;

import lombok.AllArgsConstructor;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@AllArgsConstructor
public enum TomatoRpcCoreErrorEnum {

    RPC_CONFIG_INITIALIZING_ERROR(10000, "rpc config is null"),
    ;

    private int code;
    private String message;

    public TomatoRpcErrorInfo create() {
        return new TomatoRpcErrorInfo(this.code, this.message);
    }

    public TomatoRpcErrorInfo create(String customMessage) {
        return new TomatoRpcErrorInfo(this.code, customMessage);
    }
}
