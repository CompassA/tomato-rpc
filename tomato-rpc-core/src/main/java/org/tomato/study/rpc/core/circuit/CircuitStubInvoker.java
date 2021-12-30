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

import org.tomato.study.rpc.core.StubInvoker;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.lang.reflect.Method;

/**
 * 熔断包装器
 * @author Tomato
 * Created on 2021.12.03
 */
public class CircuitStubInvoker implements StubInvoker {

    private final StubInvoker stubInvoker;
    private final CircuitBreaker breaker = SpiLoader.getLoader(CircuitBreaker.class).load();

    /**
     * 指定熔断比例
     * @param stubInvoker 被包装的客户端stub
     * @param circuitThresholdRate 熔断阈值
     */
    public CircuitStubInvoker(StubInvoker stubInvoker, int circuitThresholdRate, long periodNanos) {
        this.stubInvoker = stubInvoker;
        this.breaker.resetThresholdRate(circuitThresholdRate);
        this.breaker.resetPeriod(periodNanos);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (!breaker.allow()) {
            throw new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_CIRCUIT_ERROR.create());
        }
        try {
            Object result = stubInvoker.invoke(proxy, method, args);
            breaker.addSuccess();
            return result;
        } catch (Throwable e) {
            breaker.addFailure();
            throw e;
        }
    }

    @Override
    public String getMicroServiceId() {
        return stubInvoker.getMicroServiceId();
    }

    @Override
    public String getGroup() {
        return stubInvoker.getGroup();
    }

    @Override
    public Class<?> getServiceInterface() {
        return stubInvoker.getServiceInterface();
    }
}
