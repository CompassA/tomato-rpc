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
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.data.Code;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;

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
        return (T) Proxy.newProxyInstance(
                JdkStubFactory.class.getClassLoader(),
                new Class[] { config.getServiceInterface() },
                new StubHandler(
                        config.getServiceVIP(),
                        config.getVersion(),
                        config.getNameService(),
                        config.getServiceInterface())
        );
    }

    @AllArgsConstructor
    private static class StubHandler implements InvocationHandler {

        /**
         * 目标服务的唯一标识
         */
        private final String vip;

        /**
         * 服务版本
         */
        private final String version;

        /**
         * 注册中心
         */
        private final NameService nameService;

        /**
         * 服务接口
         */
        private final Class<?> serviceInterface;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Invocation invocation = RpcRequest.builder()
                    .serviceVIP(this.vip)
                    .interfaceName(this.serviceInterface.getName())
                    .methodName(method.getName())
                    .argsType(method.getParameterTypes())
                    .returnType(method.getReturnType())
                    .parameters(args)
                    .build();
            Response response = this.nameService.lookupInvoker(this.vip, this.version)
                    .orElseThrow(() -> new TomatoRpcRuntimeException(
                            NettyRpcErrorEnum.STUB_INVOKER_SEARCH_ERROR.create(
                                    "invoker not found, vip=" + this.version + ",version=" + this.version)
                            )
                    )
                    .invoke(invocation)
                    .getResultSync();
            if (!Code.SUCCESS.equals(response.getCode())) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.STUB_INVOKER_RPC_ERROR.create(
                                "rpc failed, server message: " + response.getMessage()
                        )
                );
            }
            return response.getData();
        }
    }
}
