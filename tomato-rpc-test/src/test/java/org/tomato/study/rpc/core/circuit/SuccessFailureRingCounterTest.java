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
import org.tomato.study.rpc.common.utils.ReflectUtils;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;

/**
 * @author Tomato
 * Created on 2022.01.31
 */
public class SuccessFailureRingCounterTest {

    @Test
    public void counterTest() {
        int size = 10;
        SuccessFailureRingCounter counter = new SuccessFailureRingCounter(size);
        Assert.assertEquals(counter.length(), size);
        for (int i = 0; i < 100; ++i) {
            if (i % 2 == 1) {
                counter.addSuccess();
            } else {
                counter.addFailure();
            }
        }
        // 010101..... -> 1110010...
        counter.addSuccess();
        counter.addSuccess();
        counter.addSuccess();
        counter.addFailure();
        Assert.assertEquals(counter.successSum(), size/2 + 1);
        Assert.assertEquals(counter.failureSum(), size/2 - 1);
    }

    @Test
    public void multiThreadTest() throws InterruptedException {
        int size = 1000;
        SuccessFailureRingCounter counter = new SuccessFailureRingCounter(size);

        int threadNum = 10;
        CountDownLatch startWait = new CountDownLatch(1);
        CountDownLatch mainWait = new CountDownLatch(threadNum);
        int fixedFailureNum = 21;
        for (int i = 0; i < threadNum; ++i) {
            new Thread(() -> {
                try {
                    startWait.await();
                    int times = size / threadNum;
                    for (int j = 0; j < fixedFailureNum; ++j) {
                        counter.addFailure();
                    }
                    for (int j = 0; j < times - fixedFailureNum; ++j) {
                        counter.addSuccess();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mainWait.countDown();
            }).start();
        }
        startWait.countDown();
        mainWait.await();

        Assert.assertEquals(fixedFailureNum * threadNum, counter.failureSum());
        Assert.assertEquals(size - fixedFailureNum * threadNum, counter.successSum());


        // 测试多线程遇到循环临界点下标时的情况
        int tNum = 4;
        CountDownLatch a = new CountDownLatch(1);
        CountDownLatch b = new CountDownLatch(tNum);
        for (int i = 0; i < tNum; ++i) {
            new Thread(() -> {
                try {
                    a.await();
                    counter.addFailure();
                    b.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        a.countDown();
        b.await();

        BitSet innerSet = ReflectUtils.reflectGet(counter, SuccessFailureRingCounter.class, "bitSet");
        for (int i = 0; i < tNum; ++i) {
            Assert.assertFalse(innerSet.get(i));
        }

        // 全部清空
        int t = 10;
        CountDownLatch c = new CountDownLatch(1);
        CountDownLatch d = new CountDownLatch(t);
        for (int i = 0; i < t; ++i) {
            new Thread(() -> {
                try {
                    c.await();
                    for (int j = 0; j < size / 2; ++j) {
                        counter.addSuccess();
                    }
                    d.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                d.countDown();
            }).start();
        }
        c.countDown();
        d.await();

        Assert.assertEquals(0, counter.failureSum());
        Assert.assertEquals(size, counter.successSum());
    }
}
