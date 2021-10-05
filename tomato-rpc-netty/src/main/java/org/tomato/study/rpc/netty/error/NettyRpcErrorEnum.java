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
    LIFE_CYCLE_START_ERROR(10002, "start rpc core service error"),
    LIFE_CYCLE_STOP_ERROR(10003, "stop rpc core service error"),
    LIFE_CYCLE_INIT_ERROR(10004, "init rpc core service error"),

    NETTY_REQUEST_HANDLE_ERROR(20001, "rpc server handler request error"),
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

    @Override
    public String toString() {
        return String.format("[error code: %d, error message: %s]", this.code, this.message);
    }
}
