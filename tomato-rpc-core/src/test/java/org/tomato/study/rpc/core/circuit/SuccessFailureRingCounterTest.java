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

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.utils.ReflectUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        // 定义十个线程, 每个线程记录失败21次，记录成功79次
        int threadNum = 10;
        int fixedSuccessNum = 79;
        int fixedFailureNum = 21;
        int size = (fixedFailureNum + fixedSuccessNum) * threadNum;
        ExecutorService executor = Executors.newFixedThreadPool(threadNum * 2);

        SuccessFailureRingCounter counter = new SuccessFailureRingCounter(size);
        List<CompletableFuture<Void>> futures = new ArrayList<>(0);
        for (int i = 0; i < threadNum; i++) {
            futures.add(CompletableFuture.runAsync(
                    new Runnable() {
                        @Override
                        @SneakyThrows
                        public void run() {
                            Thread.sleep(300);
                            for (int j = 0; j < fixedFailureNum; ++j) {
                                counter.addFailure();
                            }
                            for (int j = 0; j < fixedSuccessNum; ++j) {
                                counter.addSuccess();
                            }
                        }
                    }, executor)
            );
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        Assert.assertEquals(fixedFailureNum * threadNum, counter.failureSum());
        Assert.assertEquals(fixedSuccessNum * threadNum, counter.successSum());


        // 测试多线程更新时下标重置的情况
        int tNum = 10;
        futures = new ArrayList<>(0);
        for (int i = 0; i < tNum; ++i) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    Thread.sleep(300);
                    counter.addFailure();
                }
            }, executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        BitSet innerSet = ReflectUtils.reflectGet(counter, SuccessFailureRingCounter.class, "bitSet");
        for (int i = 0; i < tNum; ++i) {
            Assert.assertFalse(innerSet.get(i));
        }

        // 全部清空
        int t = 10;
        futures = new ArrayList<>(0);
        for (int i = 0; i < t; ++i) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    Thread.sleep(300);
                    for (int j = 0; j < size / 2; ++j) {
                        counter.addSuccess();
                    }
                }
            }, executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        Assert.assertEquals(0, counter.failureSum());
        Assert.assertEquals(threadNum * (fixedSuccessNum + fixedFailureNum), counter.successSum());
    }
}
