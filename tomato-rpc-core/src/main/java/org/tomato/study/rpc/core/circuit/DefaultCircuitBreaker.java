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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 熔断状态机
 * @author Tomato
 * Created on 2021.12.08
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    private static final AtomicIntegerFieldUpdater<DefaultCircuitBreaker> STATE_UPGRADER =
            AtomicIntegerFieldUpdater.newUpdater(DefaultCircuitBreaker.class, "state");

    /**
     * 熔断阈值
     */
    @Getter
    private double threshold;

    /**
     * 熔断状态
     */
    private volatile int state = CLOSE;

    /**
     * 成功计数器
     */
    private final SuccessFailureRingCounter counter;

    /**
     * 熔断间隔
     */
    @Getter
    private long periodNano;

    /**
     * 成功失败的统计窗口
     */
    @Getter
    private final int ringLength;

    /**
     * 熔断结束时间
     */
    @Getter
    private long endCircuitTimestamp;

    public DefaultCircuitBreaker(Double threshold, Long periodNano, Integer ringLength) {
        this.threshold = threshold;
        this.periodNano = periodNano;
        this.ringLength = ringLength;
        this.counter = new SuccessFailureRingCounter(ringLength);
        for (int i = 0; i < counter.length(); ++i) {
            counter.addSuccess();
        }
    }

    @Override
    public boolean allow() {
        switch (state) {
            case CLOSE:
            case HALF_OPEN:
                return true;
            case OPEN:
                return handleOpen();
            default:
                return false;
        }
    }

    private boolean handleOpen() {
        // 若已经过了熔断冷静期，切换为半开启状态, 并重置计数
        if (System.nanoTime() <= endCircuitTimestamp) {
            return false;
        }
        STATE_UPGRADER.compareAndSet(this, state, HALF_OPEN);
        return true;
    }

    @Override
    public void addSuccess() {
        counter.addSuccess();
        if (state == HALF_OPEN) {
            double errorRate = calcErrorRate();
            if (errorRate < threshold) {
                STATE_UPGRADER.compareAndSet(this, state, CLOSE);
            }
        }
    }

    @Override
    public void addFailure() {
        counter.addFailure();
        if (state != OPEN) {
            double errorRate = calcErrorRate();
            // 若错误率大于阈值且熔断器未开启，开启熔断
            if (errorRate >= threshold && STATE_UPGRADER.compareAndSet(this, state, OPEN)) {
                endCircuitTimestamp = System.nanoTime() + periodNano;
            }
        }
    }

    @Override
    public void resetThresholdRate(int threshold) {
        if (threshold < 0) {
            this.threshold = 0;
        } else if (threshold > 100) {
            this.threshold = 1;
        } else {
            this.threshold = 1.0 * threshold / 100;
        }
    }

    @Override
    public void resetPeriod(long periodNano) {
        this.periodNano = periodNano;
    }

    @Override
    public CircuitStatus getStatus() {
        return new CircuitStatus() {
            @Override
            public int getState() {
                return DefaultCircuitBreaker.this.state;
            }

            @Override
            public double failureRate() {
                return calcErrorRate();
            }
        };
    }

    private double calcErrorRate() {
        return 1.0 * counter.failureSum() / (counter.length());
    }
}
