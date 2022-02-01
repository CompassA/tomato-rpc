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

package org.tomato.study.rpc.netty.router;

import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.base.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.circuit.CircuitBreaker;
import org.tomato.study.rpc.core.circuit.CircuitRpcInvoker;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.transport.RpcInvoker;
import org.tomato.study.rpc.core.transport.RpcInvokerFactory;
import org.tomato.study.rpc.netty.data.Code;

/**
 * 提供基于Netty创建Invoker的方法
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyMicroServiceSpace extends BaseMicroServiceSpace {

    /**
     * invoker创建
     */
    private final RpcInvokerFactory invokerFactory;

    private final long keepAliveMs;

    public NettyMicroServiceSpace(String microServiceId,
                                  RpcInvokerFactory invokerFactory,
                                  long keepAliveMs,
                                  long timeoutMs,
                                  RpcConfig rpcConfig) {
        super(microServiceId, rpcConfig);
        this.invokerFactory = invokerFactory;
        this.keepAliveMs = keepAliveMs;
        resetInvokerTimeout(timeoutMs);
    }

    @Override
    protected CircuitRpcInvoker doCreateCircuitBreaker(RpcInvoker invoker) {
        return new CircuitRpcInvoker(invoker, getRpcConfig()) {
            @Override
            protected void doHandle(Response response, Throwable exception, CircuitBreaker breaker) {
                if (!Code.SUCCESS.equals(response.getCode()) || exception != null) {
                    breaker.addFailure();
                    return;
                }
                breaker.addSuccess();
            }
        };
    }

    @Override
    protected RpcInvoker doCreateInvoker(MetaData metaData) {
        return invokerFactory.create(metaData, keepAliveMs, getTimeoutMs()).orElse(null);
    }
}
