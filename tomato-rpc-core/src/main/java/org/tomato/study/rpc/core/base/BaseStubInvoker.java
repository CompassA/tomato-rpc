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

package org.tomato.study.rpc.core.base;

import lombok.Getter;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.StubInvoker;
import org.tomato.study.rpc.core.data.StubConfig;

import java.lang.reflect.Method;

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
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 将方法参数转化为可序列化的DTO对象
        Invocation invocation = createInvocation(method, args);

        // 调用Invoker
        Response response = doInvoke(invocation);

        // 转化为接口返回对象
        return response.getData();
    }

    /**
     * convert stub method call parameters to RPC invocation
     * @param method stub method
     * @param args args
     * @return rpc invocation
     */
    protected abstract Invocation createInvocation(Method method, Object[] args);

    /**
     * do rpc invoke
     * @param invocation rpc invocation
     * @return rpc response
     */
    protected abstract Response doInvoke(Invocation invocation);
}
