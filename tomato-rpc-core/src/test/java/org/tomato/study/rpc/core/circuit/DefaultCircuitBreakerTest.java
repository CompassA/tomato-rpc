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

import java.util.Map;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2022.01.31
 */
public class DefaultCircuitBreakerTest {

    private final String mockInterfaceName = "testInterfaceName";

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
            public String getGroup() {return null;}
            @Override
            public MetaData getMetadata() {return null;}
            @Override
            public Serializer getSerializer() {return null;}
            @Override
            public Result invoke(Invocation invocation) throws TomatoRpcException {
                if (true) { throw new RuntimeException("error"); }
                return null;
            }
            @Override
            public boolean isUsable() {return true;}
            @Override
            public void destroy() throws TomatoRpcException {}
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

        Map<String, CircuitBreaker> breakerMap = circuitRpcInvoker.getBreakerMap();
        for (int i = 0; i < config.getCircuitWindow() / 2; ++i) {
            if (i == 0) {
                Assert.assertNull(breakerMap.get(mockInterfaceName));
            } else {
                Assert.assertNotNull(breakerMap.get(mockInterfaceName));
                Assert.assertTrue(breakerMap.get(mockInterfaceName).allow());
            }
            invokeError(circuitRpcInvoker);
        }

        CircuitBreaker circuitBreaker = breakerMap.get(mockInterfaceName);
        Assert.assertNotNull(circuitBreaker);
        Assert.assertFalse(circuitBreaker.allow());

        // 睡眠，进入半开启状态
        Thread.sleep(seconds * 1000 + 1);
        Assert.assertSame(circuitBreaker, breakerMap.get(mockInterfaceName));
        Assert.assertTrue(circuitBreaker.allow());

        // 半开启后再次调用，仍然报错会被立马熔断
        invokeError(circuitRpcInvoker);
        Assert.assertSame(circuitBreaker, breakerMap.get(mockInterfaceName));
        Assert.assertTrue(circuitBreaker.allow());

    }

    private void invokeError(CircuitRpcInvoker circuitRpcInvoker) {
        try {
            circuitRpcInvoker.invoke(new Invocation() {
                public String getMicroServiceId() {return null;}
                public String getInterfaceName() {return mockInterfaceName;}
                public String getMethodName() {return null;}
                public String[] getArgsTypes() {return new String[0];}
                public Object[] getArgs() {return new Object[0];}
                public String getReturnType() {return null;}
                public Map<String, String> fetchContextMap() {return null;}
                public void putContextParameter(String key, String value) {}
                public Optional<String> fetchContextParameter(String key) {return Optional.empty();}
                public Invocation cloneInvocationWithoutContext() {return null;}
            });
        } catch (Throwable e) {
            // do nothing
        }
    }
}
