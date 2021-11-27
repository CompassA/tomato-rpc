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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.StubInvoker;

import java.lang.reflect.Method;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
@AllArgsConstructor
public abstract class BaseStubInvoker implements StubInvoker {

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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Invocation invocation = createInvocation(method, args);
        Response response = doInvoke(invocation);
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
