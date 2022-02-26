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

package org.tomato.study.rpc.netty.invoker;

import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.base.BaseStubInvoker;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.netty.data.RpcRequestDTO;

import java.lang.reflect.Method;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
public abstract class NettyBaseStubInvoker extends BaseStubInvoker {

    public NettyBaseStubInvoker(StubConfig<?> stubConfig) {
        super(stubConfig);
    }

    @Override
    protected Invocation createInvocation(Method method, Object[] args) {
        // 收集调用数据
        RpcRequestDTO rpcInvocation = createRpcInvocation(method, args);

        // 塞参数
        putParameter(rpcInvocation);

        return rpcInvocation;
    }

    protected void putParameter(RpcRequestDTO rpcInvocation) {
        StubConfig<?> stubConfig = getStubConfig();
        rpcInvocation.putContextParameter(RpcParameterKey.TIMEOUT, String.valueOf(stubConfig.getTimeoutMs()));
        rpcInvocation.putContextParameter(RpcParameterKey.COMPRESS, String.valueOf(stubConfig.getTimeoutMs()));
    }

    protected RpcRequestDTO createRpcInvocation(Method method, Object[] args) {
        RpcRequestDTO.RpcRequestDTOBuilder builder = RpcRequestDTO.builder()
                .microServiceId(getMicroServiceId())
                .interfaceName(getServiceInterface().getName())
                .methodName(method.getName())
                .returnType(method.getReturnType().getName())
                .args(args == null ? new Object[0] : args);
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            builder.argsTypes(new String[0]);
        } else {
            String[] parameterTypeNames = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                parameterTypeNames[i] = parameterTypes[i].getName();
            }
            builder.argsTypes(parameterTypeNames);
        }
        return builder.build();
    }
}
