package org.tomato.study.rpc.core.error;

import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class TomatoRpcException extends Exception {

    @Getter
    private final String message;

    public TomatoRpcException(String message) {
        super();
        this.message = message;
    }
}
