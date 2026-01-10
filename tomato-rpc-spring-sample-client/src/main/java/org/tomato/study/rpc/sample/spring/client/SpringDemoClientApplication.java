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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.tomato.study.rpc.config.annotation.RpcClientStub;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.ExtensionHeader;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.ExecutionChainFacade;
import org.tomato.study.rpc.sample.api.SumService;
import org.tomato.study.rpc.sample.api.data.Constant;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.ExecutionChainRequest;
import org.tomato.study.rpc.sample.api.data.ExecutionChainResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
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

    public static final String CLIENT_MODE_KEY = "tomato-rpc.client-mode";
    public static final String ECHO_MODE = "echo";
    public static final String SUM_MODE = "sum";
    public static final String GRAY_ROUTER_TEST = "router";

    @RpcClientStub(microServiceId = Constant.serviceId, compressBody = true, timeout = 1000)
    private EchoService echoService;

    @RpcClientStub(microServiceId = Constant.serviceId, compressBody = true, timeout = 5000)
    private SumService sumService;

    @RpcClientStub(microServiceId = Constant.serviceId)
    private ExecutionChainFacade executionChainFacade;

    public String echo(String msg) {
        return echoService.echo(new DemoRequest(msg)).getData();
    }

    public Integer sum(List<Integer> nums) {
        return sumService.sum(nums);
    }

    public ExecutionChainResponse dispatch(ExecutionChainRequest request) {
        return executionChainFacade.dispatch(request);
    }

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(SpringDemoClientApplication.class);
        SpringDemoClientApplication bean = context.getBean(SpringDemoClientApplication.class);
        RpcCoreService rpcCoreService = context.getBean(RpcCoreService.class);

        Runnable runnable = createRunnable(bean, rpcCoreService);

        int threadNum = 12;
        int messageNum = 1000000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadNum, threadNum, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; ++i) {
            executor.execute(() -> {
                for (int j = 0; j < messageNum; ++j) {
                    InvocationContext.initContext();
                    MDC.put(ExtensionHeader.TRACE_ID.name(), ExtensionHeader.TRACE_ID.getValueFromContext());
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        log.error("rpc client error", e);
                    } finally {
                        InvocationContext.remove();
                        MDC.clear();
                    }

                    try {
                        Thread.sleep(Math.round(2000 * (Math.random()-0.5) + 2500));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        System.exit(0);
    }

    private static Runnable createRunnable(SpringDemoClientApplication bean,
                                           RpcCoreService rpcCoreService) {
        String clientMode = System.getProperty(CLIENT_MODE_KEY);

        log.info("client run mode: {}", clientMode);

        if (StringUtils.isBlank(clientMode) || GRAY_ROUTER_TEST.equals(clientMode)) {
            return () -> {
                InvocationContext.put("USER_ID", "2323232321002");
                MetaData rpcServerMetaData = rpcCoreService.getRpcServerMetaData();
                String asciiString = MetaData.convert(rpcServerMetaData).get().toASCIIString();

                log.info("请求发起");
                ExecutionChainRequest request = new ExecutionChainRequest();
                request.setServerURLList(List.of(asciiString));

                ExecutionChainResponse response = bean.dispatch(request);
                StringBuilder builder = new StringBuilder();
                List<String> serverURLList = response.getServerURLList();
                for (int i = 0; i < serverURLList.size(); i++) {
                    String url = serverURLList.get(i);
                    MetaData metaData = MetaData.convert(URI.create(url)).get();
                    builder.append(String.format("【micro-service-id=%s,stage=%s,group=%s,host=%s,port=%d】\n",
                        metaData.getMicroServiceId(),
                        metaData.getStage(),
                        metaData.getGroup(),
                        metaData.getHost(),
                        metaData.getPort()
                    ));
                    if (i != serverURLList.size() - 1) {
                        builder.append(" ");
                        for (int j = 0; j <= i; ++j) {
                            builder.append("--");
                        }
                        builder.append("> ");
                    }
                }
                log.info("\n请求结束, 调用链: \n{}", builder);
            };
        } else if (SUM_MODE.equals(clientMode)) {
            return () -> {
                int base = 10000;
                List<Integer> nums = Arrays.asList(
                    (int) Math.round(Math.random() * base),
                    (int) Math.round(Math.random() * base),
                    (int) Math.round(Math.random() * base),
                    (int) Math.round(Math.random() * base));
                log.info("sum of {} is {}", nums, bean.sum(nums));
            };
        } else if (ECHO_MODE.equals(clientMode)) {
            return () -> log.info(bean.echo("hello world"));
        }

        return null;
    }
}
