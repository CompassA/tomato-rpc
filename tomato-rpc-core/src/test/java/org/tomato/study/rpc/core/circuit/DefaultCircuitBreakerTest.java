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

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.transport.RpcInvoker;

/**
 * @author Tomato
 * Created on 2022.01.31
 */
public class DefaultCircuitBreakerTest {

    @Test
    public void circuitBreakerTest() throws InterruptedException {
        long intervalNano = 1_000_000_000L;
        DefaultCircuitBreaker breaker = new DefaultCircuitBreaker(0.4, intervalNano, 10);
        for (int i = 0; i < 3; ++i) {
            breaker.addFailure();
            Assert.assertTrue(breaker.allow());
        }
        breaker.addFailure();
        Assert.assertFalse(breaker.allow());
        Assert.assertEquals(breaker.getStatus().failureRate(), 4.0 / breaker.getRingLength(), 0.0001);

        Thread.sleep(intervalNano / 1000_000 + 1);

        Assert.assertTrue(breaker.allow());
        Assert.assertEquals(breaker.getStatus().getState(), CircuitBreaker.HALF_OPEN);


        for (int i = 0; i < 10; ++i) {
            breaker.addSuccess();
        }
        Assert.assertTrue(breaker.allow());
        Assert.assertEquals(breaker.getStatus().getState(), CircuitBreaker.CLOSE);
        Assert.assertEquals(breaker.getStatus().failureRate(), 0, 0.00001);

    }

    @Test
    public void invokerTest() throws TomatoRpcException, InterruptedException {
        RpcInvoker mockInvoker = new RpcInvoker() {
            @Override
            public String getGroup() {
                return null;
            }

            @Override
            public MetaData getMetadata() {
                return null;
            }

            @Override
            public long getInvocationTimeout() {
                return 0;
            }

            @Override
            public void setInvocationTimeout(long timeoutMs) {

            }

            @Override
            public Serializer getSerializer() {
                return null;
            }

            @Override
            public Result invoke(Invocation invocation) throws TomatoRpcException {
                if (true) {
                    throw new RuntimeException("error");
                }
                return null;
            }

            @Override
            public void destroy() throws TomatoRpcException {

            }
        };

        long seconds = 3;
        RpcConfig config = RpcConfig.builder()
                .enableCircuit(true)
                .circuitOpenRate(0.5)
                .circuitOpenSeconds(seconds)
                .circuitWindow(100)
                .build();
        CircuitRpcInvoker circuitRpcInvoker = new CircuitRpcInvoker(mockInvoker, config) {
            @Override
            protected void doHandle(Response response, Throwable exception, CircuitBreaker breaker) {
                if (exception != null) {
                    breaker.addFailure();
                }
            }
        };

        for (int i = 0; i < config.getCircuitWindow() / 2; ++i) {
            Assert.assertTrue(circuitRpcInvoker.allow());
            invokeError(circuitRpcInvoker);
        }

        Assert.assertFalse(circuitRpcInvoker.allow());

        // 睡眠，进入半开启状态
        Thread.sleep(seconds * 1000 + 1);
        Assert.assertTrue(circuitRpcInvoker.allow());

        // 半开启后再次调用，仍然报错会被立马熔断
        invokeError(circuitRpcInvoker);
        Assert.assertFalse(circuitRpcInvoker.allow());

    }

    private void invokeError(CircuitRpcInvoker circuitRpcInvoker) {
        try {
            circuitRpcInvoker.invoke(null);
        } catch (Throwable e) {
            // do nothing
        }
    }
}
