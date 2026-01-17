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

package org.tomato.study.rpc.core.circuit;

import lombok.Getter;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.Response;
import org.tomato.study.rpc.core.data.Result;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.serializer.Serializer;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invoker的熔断包装器
 * @author Tomato
 * Created on 2022.02.01
 */
public abstract class CircuitRpcInvoker implements RpcInvoker {

    private final RpcInvoker rpcInvoker;
    private final RpcConfig rpcConfig;
    private final CircuitBreakerFactory factory;

    /**
     * api-id -> breaker;
     */
    @Getter
    private final Map<String, CircuitBreaker> breakerMap = new ConcurrentHashMap<>(0);

    public CircuitRpcInvoker(RpcInvoker rpcInvoker, RpcConfig rpcConfig) {
        this.rpcInvoker = rpcInvoker;
        this.rpcConfig = rpcConfig;
        this.factory = SpiLoader.getLoader(CircuitBreakerFactory.class).load();
    }

    @Override
    public String getGroup() {
        return rpcInvoker.getGroup();
    }

    @Override
    public MetaData getMetadata() {
        return rpcInvoker.getMetadata();
    }

    @Override
    public Serializer getSerializer() {
        return rpcInvoker.getSerializer();
    }

    @Override
    public Result invoke(Invocation invocation) throws TomatoRpcException {
        CircuitBreaker breaker = breakerMap.computeIfAbsent(
                invocation.getApiId(),
                (key) -> factory.createBreaker(rpcConfig));
        if (!breaker.allow()) {
            CircuitStatus status = breaker.getStatus();
            throw new TomatoRpcException(TomatoRpcErrorEnum.RPC_CIRCUIT_ERROR, String.format(
                "micro-service=%s, breaker-status=%s, failureRate=%f", invocation.getMicroServiceId(), status.getState(), status.failureRate()));
        }
        try {
            Result result = rpcInvoker.invoke(invocation);

            // 注册回调, 不同的response可能有不同的编码方式和code表示方式
            result.getResultAsync().handleAsync((response, exception) -> {
                doHandle(response, exception, breaker);
                return response;
            });
            return result;
        } catch (TomatoRpcException | RuntimeException e) {
            breaker.addFailure();
            throw e;
        }
    }

    @Override
    public void destroy() throws TomatoRpcException {
        rpcInvoker.destroy();
    }

    @Override
    public boolean isUsable() {
        return rpcInvoker.isUsable();
    }

    /**
     * 方法调用完成后处理熔断
     * @param response 调用结果
     * @param exception 异常
     * @param breaker 断路器
     */
    protected abstract void doHandle(Response response, Throwable exception, CircuitBreaker breaker);
}
