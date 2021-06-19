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
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {

    public int code;

    public String message;

    public Object data;

    public static RpcResponse success(Object data) {
        return new RpcResponse(Code.SUCCESS.getCode(), Code.SUCCESS.getMsg(), data);
    }

    public static RpcResponse fail(Object data) {
        return fail(data, Code.FAIL.getMsg());
    }

    public static RpcResponse fail(Object data, String message) {
        return new RpcResponse(Code.FAIL.getCode(), message, data);
    }
}
