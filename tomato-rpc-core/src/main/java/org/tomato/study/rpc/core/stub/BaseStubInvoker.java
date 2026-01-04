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

package org.tomato.study.rpc.core.stub;

import lombok.Getter;
import org.tomato.study.rpc.core.RpcParameterKey;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.Response;
import org.tomato.study.rpc.core.data.RpcRequestDTO;
import org.tomato.study.rpc.core.data.StubConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
public abstract class BaseStubInvoker implements StubInvoker {

    @Getter
    private final StubConfig<?> stubConfig;

    /**
     * 目标服务的唯一标识
     */
    @Getter
    private final String microServiceId;

    /**
     * 服务分组
     */
    @Getter
    @Deprecated
    private final String group;

    /**
     * 服务接口
     */
    @Getter
    private final Class<?> serviceInterface;

    public BaseStubInvoker(StubConfig<?> stubConfig) {
        this.stubConfig = stubConfig;
        this.microServiceId = stubConfig.getMicroServiceId();
        this.group = stubConfig.getGroup();
        this.serviceInterface = stubConfig.getServiceInterface();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(proxy, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return this.toString();
            } else if ("hashCode".equals(methodName)) {
                return this.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return proxy.equals(args[0]);
        }

        // 作为上游的被调用方, 调用下游的接口, 此时需要保留现场
        Map<String, String> originContext = InvocationContext.get();
        Map<String, String> currentContext = originContext == null ? new HashMap<>() : new HashMap<>(originContext);
        InvocationContext.set(currentContext);
        try {
            // 塞参数
            putParameter(currentContext);

            // 将方法参数转化为可序列化的DTO对象
            Invocation invocation = createInvocation(method, args);

            // 调用
            Response response = doInvoke(invocation);

            // 转化为接口返回对象
            return response.getData();
        } finally {
            InvocationContext.set(originContext);
        }
    }

    /**
     * 塞一些通用的参数
     */
    private void putParameter(Map<String, String> threadLocalParameter) {
        threadLocalParameter.put(RpcParameterKey.TIMEOUT, String.valueOf(stubConfig.getTimeoutMs()));
        threadLocalParameter.put(RpcParameterKey.COMPRESS, String.valueOf(stubConfig.isCompressBody()));
    }

    protected Invocation createInvocation(Method method, Object[] args) {
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

    /**
     * do rpc invoke
     * @param invocation rpc invocation
     * @return rpc response
     */
    protected abstract Response doInvoke(Invocation invocation);
}
