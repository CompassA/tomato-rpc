package org.tomato.study.rpc.netty.error;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.error.TomatoRpcErrorInfo;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@AllArgsConstructor
public enum NettyRpcErrorEnum {

    CORE_SERVICE_REGISTER_PROVIDER_ERROR(10000, "register provider parameter error"),
    CORE_SERVICE_STUB_CREATE_ERROR(10001, "create stub parameter error"),
    CORE_SERVICE_START_ERROR(10002, "start rpc core service error"),

    NETTY_HANDLER_WRITE_ERROR(20001, "netty response write failed"),
    NETTY_HANDLER_PROVIDER_NOT_FOUND(20002, "provider interface not found"),
    NETTY_HANDLER_RPC_INVOKER_ERROR(20003, "rpc invoke error"),

    STUB_INVOKER_SEARCH_ERROR(30001, "invoker not found"),
    STUB_INVOKER_RPC_ERROR(30002, "client rpc failed"),

    MODEL_DTO_CONVERT_ERROR(40001, "rpc request convert error"),
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
