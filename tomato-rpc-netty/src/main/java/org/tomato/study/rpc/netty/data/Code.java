package org.tomato.study.rpc.netty.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@AllArgsConstructor
public enum Code {

    UNKNOWN(-1, "UNKNOWN"),
    SUCCESS(0, "SUCCESS"),
    FAIL(1, "FAIL"),
    ;

    private final int code;
    private final String msg;

    public boolean equals(int code) {
        return this.code == code;
    }

    public static Code valueOf(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (Code value : values()) {
            if (code.equals(value.getCode())) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
