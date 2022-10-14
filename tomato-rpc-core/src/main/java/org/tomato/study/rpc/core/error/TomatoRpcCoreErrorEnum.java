/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@Getter
@AllArgsConstructor
public enum TomatoRpcCoreErrorEnum {

    RPC_CONFIG_INITIALIZING_ERROR(10000, "rpc config is null"),
    RPC_CLIENT_TIMEOUT(10002, "rpc timeout"),
    RPC_CIRCUIT_ERROR(10003, "circuit breaker is open"),
    RPC_INVOKER_CLOSED(10004, "rpc invoker and net connection have been closed"),


    RPC_ROUND_ROBIN_LOAD_BALANCE_ERROR(20001, "round robin error"),

    STUB_INVOKER_SEARCH_ERROR(30001, "invoker not found"),
    STUB_INVOKER_RPC_ERROR(30002, "client rpc failed"),
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
