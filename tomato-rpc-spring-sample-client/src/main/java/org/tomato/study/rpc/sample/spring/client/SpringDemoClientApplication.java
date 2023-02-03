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

package org.tomato.study.rpc.sample.spring.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.tomato.study.rpc.config.annotation.RpcClientStub;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.SumService;
import org.tomato.study.rpc.sample.api.data.DemoRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Tomato
 * Created on 2021.11.20
 */
@Slf4j
@Component
@SpringBootApplication
public class SpringDemoClientApplication {

    @RpcClientStub(compressBody = true, timeout = 1000)
    private EchoService echoService;

    @RpcClientStub(compressBody = true, timeout = 5000)
    private SumService sumService;

    public String echo(String msg) {
        return echoService.echo(new DemoRequest(msg)).getData();
    }

    public Integer sum(List<Integer> nums) {
        return sumService.sum(nums);
    }

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(SpringDemoClientApplication.class);
        SpringDemoClientApplication bean = context.getBean(SpringDemoClientApplication.class);
        int threadNum = 10;
        int messageNum = 1000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadNum, threadNum, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; ++i) {
            executor.execute(() -> {
                Random random = new Random();
                for (int j = 0; j < messageNum; ++j) {
                    process(bean, random, j);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        Random random = new Random();
        for (int i = 0; i < 100000000; ++i) {
            process(bean, random, i);
            Thread.sleep(1500);
        }
        context.close();
        System.exit(0);
    }

    private static void process(SpringDemoClientApplication bean, Random random, int i) {
        try {
            if ((i & 1) == 0) {
                log.info(bean.echo("hello world"));
            } else {
                List<Integer> nums = Arrays.asList(
                        random.nextInt(10000),
                        random.nextInt(10000),
                        random.nextInt(20000),
                        random.nextInt(23232));
                log.info("sum of {} is {}", nums, bean.sum(nums));
            }
        } catch (Exception e) {
            log.error("rpc client error", e);
        }
    }

}
