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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
@Getter
@AllArgsConstructor
public enum TomatoRpcErrorEnum {
    UNKNOWN(-1, "unknown"),
    SUCCESS(0, "success"),

    MICROSERVICE_ID_NOT_FOUND(1, "missing micro-service-id"),
    RPC_CORE_SERVICE_BEAN_START_ERROR(2, "create rpc core service bean error"),
    RPC_CORE_SERVICE_STOP_ERROR(3, "stop rpc core service bean error"),

    RPC_CONFIG_INITIALIZING_ERROR(10000, "rpc config is null"),
    RPC_INVOCATION_TIMEOUT(10002, "client rpc invocation timeout"),
    RPC_CIRCUIT_ERROR(10003, "circuit breaker is open"),
    RPC_INVOKER_CLOSED(10004, "rpc invoker and net connection have been closed"),
    RPC_COMPONENT_LIFE_CYCLE_INVALID_STATE(10005, "invalid rpc component state"),
    RPC_CONNECTION_TIMEOUT(10006, "connect timeout"),
    RPC_REGISTRY_CLOSE_ERROR(10007, "rpc registry close failed"),

    RPC_ROUND_ROBIN_LOAD_BALANCE_ERROR(20001, "round robin error"),
    RPC_SERIALIZE_ERROR(20002, "json serialize failed"),
    RPC_ROUTER_REFRESH_ERROR(20003, "rpc router refresh error"),

    STUB_INVOKER_SEARCH_ERROR(30001, "invoker not found"),
    SERVICE_STUB_CREATE_ERROR(30002, "service stub register failed"),

    CORE_SERVICE_REGISTER_PROVIDER_ERROR(40000, "register provider parameter error"),
    CORE_SERVICE_STUB_CREATE_ERROR(40001, "create stub parameter error"),
    LIFE_CYCLE_START_ERROR(40002, "start rpc core service error"),
    LIFE_CYCLE_STOP_ERROR(40003, "stop rpc core service error"),
    LIFE_CYCLE_INIT_ERROR(40004, "init rpc core service error"),

    NETTY_REQUEST_HANDLE_ERROR(50001, "rpc server handler request error"),
    NETTY_HANDLER_PROVIDER_NOT_FOUND(50002, "provider interface not found"),
    NETTY_HANDLER_RPC_INVOKER_ERROR(50003, "rpc invoke error"),
    NETTY_CLIENT_RPC_ERROR(50004, "netty network client error"),

    MODEL_DTO_CONVERT_ERROR(60001, "rpc request convert error"),
    CODEC_DECODE_ERROR(60002, "rpc frame decode error"),
    CODEC_ENCODE_ERROR(60003, "rpc frame encode error"),
    ;

    private static final Map<Integer, TomatoRpcErrorEnum> CODE_MAP = new HashMap<>();
    static {
        for (TomatoRpcErrorEnum value : TomatoRpcErrorEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final int code;
    private final String message;


    public static TomatoRpcErrorEnum valueOfCode(int code) {
        return CODE_MAP.getOrDefault(code, TomatoRpcErrorEnum.UNKNOWN);
    }
}
