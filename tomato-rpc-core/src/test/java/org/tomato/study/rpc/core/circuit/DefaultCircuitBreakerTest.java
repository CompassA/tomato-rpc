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
}
