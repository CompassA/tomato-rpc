package org.tomato.study.rpc.registry.zookeeper.error;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.error.TomatoRpcErrorInfo;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@AllArgsConstructor
public enum TomatoRegistryErrorEnum {

    RPC_REGISTRY_CLOSE_ERROR(10000, "rpc registry close failed"),
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
