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

        // create rpc core service
        RpcCoreService rpcCoreService = createCoreService(zkURL);

        // create stub
        EchoService stub = createStub(rpcCoreService);

        // call RPC method 3 seconds a time
        invokerRpc(stub);

        rpcCoreService.stop();
    }

    private static RpcCoreService createCoreService(String zkURL) throws TomatoRpcException {
        RpcCoreService rpcCoreService = SpiLoader.getLoader(RpcCoreServiceFactory.class)
                .load()
                .create(RpcConfig.builder()
                        .serviceVIP("org.tomato.study.rpc.demo.client")
                        .subscribedVIP(Collections.singletonList(Constant.serviceVIP))
                        .nameServiceURI(zkURL)
                        .port(7890)
                        .build()
                );
        rpcCoreService.init();
        rpcCoreService.start();
        return rpcCoreService;
    }

    private static EchoService createStub(RpcCoreService rpcCoreService) {
        Optional<ApiConfig<EchoService>> apiConfig = ApiConfig.create(EchoService.class);
        assert apiConfig.isPresent();
        return rpcCoreService.createStub(apiConfig.get().getServiceVIP(), apiConfig.get().getApi());
    }

    private static void invokerRpc(EchoService stub) throws InterruptedException {
        for (int i = 0; i < 20; ++i) {
            try {
                DemoResponse response = stub.echo(new DemoRequest("hello world"));
                LOGGER.info(response.getData());
            } catch (TomatoRpcRuntimeException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
            Thread.sleep(3000);
        }
    }
}
