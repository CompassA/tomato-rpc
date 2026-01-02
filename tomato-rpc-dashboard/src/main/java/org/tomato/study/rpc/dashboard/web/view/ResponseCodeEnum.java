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

package org.tomato.study.rpc.dashboard.web.view;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 * @author Tomato
 * Created on 2022.08.07
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum {
    /**
     * 成功
     */
    SUCCESS(10000, "success"),
    PARAMETER_INVALID(10001, "parameter invalid"),
    RPC_UNKNOWN_EXCEPTION(10002, "unknown exception"),



    ZOOKEEPER_CLIENT_ERROR(20001, "ZOOKEEPER"),
    ;

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应描述
     */
    private final String message;
}
