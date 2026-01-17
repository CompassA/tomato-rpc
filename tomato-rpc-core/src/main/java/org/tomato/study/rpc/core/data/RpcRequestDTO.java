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

package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Objects;

/**
 * 一次RPC调用的请求参数
 * @author Tomato
 * Created on 2021.09.30
 */
@Getter
@Setter
@Builder
@ToString
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcRequestDTO that = (RpcRequestDTO) o;
        return Objects.equals(microServiceId, that.microServiceId)
                && Objects.equals(interfaceName, that.interfaceName)
                && Objects.equals(methodName, that.methodName)
                && Arrays.equals(argsTypes, that.argsTypes)
                && Objects.equals(returnType, that.returnType)
                && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(microServiceId, interfaceName, methodName, returnType);
        result = 31 * result + Arrays.hashCode(argsTypes);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
