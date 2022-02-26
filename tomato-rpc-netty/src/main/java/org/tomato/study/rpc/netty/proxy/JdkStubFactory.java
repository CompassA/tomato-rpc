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

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.transport.RpcInvoker;
import org.tomato.study.rpc.core.transport.RpcInvokerFactory;
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
@AllArgsConstructor
public class JdkStubFactory implements StubFactory {

    private final RpcConfig rpcConfig;
    private final RpcInvokerFactory rpcInvokerFactory;

    @Override
    public <T> T createStub(StubConfig<T> config) throws IllegalStateException {
        if (config == null || !config.isValid()) {
            throw new IllegalStateException("stub config is not valid, stub config: " + config);
        }
        if (config.getNameServer() != null) {
            return createServiceDiscoveryStub(config);
        }
        MetaData nodeInfo = config.getNodeInfo();
        if (nodeInfo != null && nodeInfo.isValid()) {
            return createDirectInvoker(config);
        }
        throw new IllegalArgumentException("without nameserver to create service-discovery stud and" +
                "without node-info to create direct stub");
    }

    @SuppressWarnings("unchecked cast")
    private <T> T createDirectInvoker(StubConfig<T> config) {
        RpcInvoker rpcInvoker = rpcInvokerFactory.create(config.getNodeInfo(), rpcConfig).orElse(null);
        if (rpcInvoker == null) {
            throw new IllegalArgumentException("createDirectInvoker - create rpc invoker failed");
        }
        Class<T> serviceInterface = config.getServiceInterface();
        NettyBaseStubInvoker stubInvoker = new NettyDirectStubInvoker(config, rpcInvoker);
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[]{serviceInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return stubInvoker.invoke(proxy, method, args);
                    }
                }
        );
    }

    @SuppressWarnings("unchecked cast")
    private <T> T createServiceDiscoveryStub(StubConfig<T> config) {
        NettyBaseStubInvoker stubInvoker = new NettyRouterStubInvoker(config);
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
}
