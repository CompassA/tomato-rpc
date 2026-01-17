package org.tomato.study.rpc.core.error;

import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.09.28
 */
@Getter
public class TomatoRpcException extends Exception {

    private final TomatoRpcErrorEnum errCode;
    private final String errMsg;


    public TomatoRpcException(TomatoRpcErrorEnum errCode) {
        this(errCode, errCode.getMessage());
    }

    public TomatoRpcException(TomatoRpcErrorEnum errCode, String errMsg) {
        super(errMsg);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public TomatoRpcException(Throwable e, TomatoRpcErrorEnum errCode) {
        this(e, errCode, e.getMessage());
    }

    public TomatoRpcException(Throwable e, TomatoRpcErrorEnum errCode, String errMsg) {
        super(errMsg, e);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
}
