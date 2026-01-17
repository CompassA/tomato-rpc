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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

/**
 * necessary data during a RPC
 * @author Tomato
 * Created on 2021.03.31
 */
@Setter
@Getter
@NoArgsConstructor
public class RpcRequestModel {

    /**
     * rpc service micro-service-id in the service name registry
     */
    private String microServiceId;

    /**
     * RPC interface class full name,
     * rpc server maintains the mapping: interfaceName -> Server Provider Instance
     */
    private Class<?> rpcInterFace;

    /**
     * RPC method name
     * RPC server gets the provider instance
     * and searches the Method instance by instance.class.getMethod
     */
    private String methodName;

    /**
     * RPC method types of the arguments
     */
    private Class<?>[] argsType;

    /**
     * RPC method return type
     */
    private Class<?> returnType;

    /**
     * the arguments input from client interface
     * the order is the same as the rpc method signature
     */
    private Object[] parameters;

    /**
     * api id
     */
    private String apiId;

    @Builder
    public RpcRequestModel(String microServiceId,
                           Class<?> rpcInterFace,
                           String methodName,
                           Class<?>[] argsType,
                           Class<?> returnType,
                           String apiId,
                           Object... parameters) {
        this.microServiceId = microServiceId;
        this.rpcInterFace = rpcInterFace;
        this.methodName = methodName;
        this.argsType = argsType;
        this.returnType = returnType;
        this.apiId = apiId;
        if (parameters == null || parameters.length == 0) {
            this.parameters = new Object[0];
        } else {
            this.parameters = Arrays.copyOf(parameters, parameters.length);
        }
    }
}
