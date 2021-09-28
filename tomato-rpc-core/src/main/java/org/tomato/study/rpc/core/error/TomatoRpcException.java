package org.tomato.study.rpc.core.error;

/**
 * @author Tomato
 * Created on 2021.09.28
 */
public class TomatoRpcException extends Exception {

    private final TomatoRpcErrorInfo errorInfo;

    public TomatoRpcException(TomatoRpcErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.errorInfo = errorInfo;
    }
    public TomatoRpcException(TomatoRpcErrorInfo errorInfo, Throwable throwable) {
        super(errorInfo.getMessage(), throwable);
        this.errorInfo = errorInfo;
    }

    @Override
    public String getMessage() {
        return errorInfo.getMessage();
    }
}
