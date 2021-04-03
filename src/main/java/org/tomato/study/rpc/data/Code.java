package org.tomato.study.rpc.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@AllArgsConstructor
public enum Code {

    SUCCESS(0, "SUCCESS"),
    ;

    private final int code;
    private final String msg;

    public boolean equals(int code) {
        return this.code == code;
    }
}
