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

import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 通过jdk动态代理创建rpc stub
 * @author Tomato
 * Created on 2021.04.18
 */
public class JdkStubFactory extends BaseStubFactory {

    @Override
    @SuppressWarnings("unchecked cast")
    protected <T> T doCreateStub(StubConfig<T> config, RpcConfig rpcConfig, StubInvoker invoker) {
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[]{ config.getServiceInterface() },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return invoker.invoke(proxy, method, args);
                    }
                }
        );
    }
}
