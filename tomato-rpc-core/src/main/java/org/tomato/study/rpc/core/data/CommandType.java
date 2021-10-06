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

    /**
     * keep alive request
     */
    KEEP_ALIVE_REQUEST((short) 3),

    /**
     * keep alive response
     */
    KEEP_ALIVE_RESPONSE((short) 4),
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
