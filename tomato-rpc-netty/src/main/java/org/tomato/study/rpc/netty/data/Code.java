/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
