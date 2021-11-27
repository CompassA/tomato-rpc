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

package org.tomato.study.rpc.netty.proxy;

import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.transport.RpcInvoker;
import org.tomato.study.rpc.netty.invoker.NettyBaseStubInvoker;
import org.tomato.study.rpc.netty.invoker.NettyDirectStubInvoker;
import org.tomato.study.rpc.netty.invoker.NettyRouterStubInvoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 通过jdk动态代理创建rpc stub
 * @author Tomato
 * Created on 2021.04.18
 */
public class JdkStubFactory implements StubFactory {

    @Override
    @SuppressWarnings("unchecked cast")
    public <T> T createStub(StubConfig<T> config) {
        if (config == null || !config.isValid()) {
            return null;
        }
        NettyBaseStubInvoker stubInvoker = new NettyRouterStubInvoker(
                config.getMicroServiceId(),
                config.getGroup(),
                config.getServiceInterface(),
                config.getNameServer());
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[]{config.getServiceInterface()},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return stubInvoker.invoke(proxy, method, args);
                    }
                }
        );
    }

    @Override
    @SuppressWarnings("unchecked cast")
    public <T> T createStub(String microServiceId, RpcInvoker rpcInvoker, Class<T> rpcInterface) {
        if (rpcInvoker == null || rpcInterface == null || !rpcInterface.isInterface()) {
            return null;
        }
        NettyBaseStubInvoker stubInvoker = new NettyDirectStubInvoker(microServiceId, rpcInterface, rpcInvoker);
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[]{rpcInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return stubInvoker.invoke(proxy, method, args);
                    }
                }
        );
    }
}
