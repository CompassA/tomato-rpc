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

package org.tomato.study.rpc.netty.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.Invocation;

/**
 * 一次RPC调用的请求参数
 * @author Tomato
 * Created on 2021.09.30
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequestDTO implements Invocation {

    /**
     * {@link RpcRequestModel#getMicroServiceId()}
     */
    private String microServiceId;

    /**
     * {@link RpcRequestModel#getRpcInterFace()}
     */
    private String interfaceName;

    /**
     * {@link RpcRequestModel#getMethodName()}
     */
    private String methodName;

    /**
     * {@link RpcRequestModel#getArgsType()}
     */
    private String[] argsTypes;

    /**
     * {@link RpcRequestModel#getReturnType()}
     */
    private String returnType;

    /**
     * {@link RpcRequestModel#getParameters()}
     */
    private Object[] args;
}
