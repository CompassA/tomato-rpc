/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.sample.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcCoreServiceFactory;
import org.tomato.study.rpc.core.data.ApiConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.data.Constant;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.DemoResponse;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author Tomato
 * Created on 2021.06.21
 */
public class DemoClientApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoClientApplication.class);

    public static void main(String[] args) throws Exception {
        String zkURL = System.getenv("ZK_IP_PORT");
        if (StringUtils.isBlank(zkURL)) {
            zkURL = "127.0.0.1:2181";
        }

        // 创建RPC核心接口
        RpcCoreService rpcCoreService = createCoreService(zkURL);

        // 创建动态代理
        EchoService stub = createStub(rpcCoreService);

        // RPC调用
        if (System.getenv("PERF") == null) {
            invokerRpc(stub);
        } else {
            invokerRpcPref(stub);
        }

        rpcCoreService.stop();
    }

    private static RpcCoreService createCoreService(String zkURL) throws TomatoRpcException {
        RpcCoreService rpcCoreService = SpiLoader.getLoader(RpcCoreServiceFactory.class)
                .load()
                .create(RpcConfig.builder()
                        .serviceVIP("demo-rpc-client")
                        .subscribedVIP(Collections.singletonList(Constant.serviceId))
                        .nameServiceURI(zkURL)
                        .port(7890)
                        .useGzip(true)
                        .build()
                );
        rpcCoreService.init();
        rpcCoreService.start();
        return rpcCoreService;
    }

    private static EchoService createStub(RpcCoreService rpcCoreService) {
        Optional<ApiConfig<EchoService>> apiConfig = ApiConfig.create(EchoService.class);
        assert apiConfig.isPresent();
        return rpcCoreService.createStub(apiConfig.get());
    }

    private static void invokerRpc(EchoService stub) throws InterruptedException {
        int threadNum = 10;
        int messageNum = 100;
        CountDownLatch mainThreadWait = new CountDownLatch(threadNum);
        CountDownLatch subThreadWait = new CountDownLatch(1);
        Runnable runnable = () -> {
            try {
                subThreadWait.await();
                for (int i = 0; i < messageNum; ++i) {
                    DemoResponse response = stub.echo(new DemoRequest("hello world"));
                    LOGGER.info(response.getData());
                    Thread.sleep(500);
                }
                mainThreadWait.countDown();
            } catch (TomatoRpcRuntimeException e) {
                LOGGER.error(e.getErrorInfo().toString(), e);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        };

        for (int i = 0; i < threadNum; ++i) {
            new Thread(runnable).start();
        }

        subThreadWait.countDown();
        mainThreadWait.await();
    }

    private static void invokerRpcPref(EchoService stub) throws InterruptedException {
        int threadNum = 50;
        int messageNum = 1000;
        CountDownLatch mainThreadWait = new CountDownLatch(threadNum);
        CountDownLatch subThreadWait = new CountDownLatch(1);
        String bigBody = bigBody();
        Runnable runnable = () -> {
            try {
                subThreadWait.await();
                for (int i = 0; i < messageNum; ++i) {
                    DemoResponse response = stub.echo(new DemoRequest(bigBody));
                }
                mainThreadWait.countDown();
            } catch (TomatoRpcRuntimeException e) {
                LOGGER.error(e.getErrorInfo().toString(), e);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        };


        for (int i = 0; i < threadNum; ++i) {
            new Thread(runnable).start();
        }


        LOGGER.info(
                "RPC perf test started, thread num: {}, messages per thread {}",
                threadNum,
                messageNum
        );
        long start = System.nanoTime();
        subThreadWait.countDown();
        mainThreadWait.await();
        long end = System.nanoTime();
        LOGGER.info(
                "RPC perf test stopped, invoke {} times RPC cost {} s",threadNum * messageNum,
                (end - start) / 1_000_000_000.0);
    }

    private static String bigBody() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            builder.append(UUID.randomUUID());
        }
        return builder.toString();
    }
}
