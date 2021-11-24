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

package org.tomato.study.rpc.sample.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcCoreServiceFactory;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.data.Constant;

/**
 * @author Tomato
 * Created on 2021.06.20
 */
@Slf4j
public class DemoServerApplication {

    public static void main(String[] args) throws Exception {
        String zkURL = System.getenv("ZK_IP_PORT");
        if (StringUtils.isBlank(zkURL)) {
            zkURL = "127.0.0.1:2181";
        }
        RpcCoreService coreService = SpiLoader.getLoader(RpcCoreServiceFactory.class)
                .load()
                .create(RpcConfig.builder()
                        .microServiceId(Constant.serviceId)
                        .nameServiceURI(zkURL)
                        .port(4567)
                        .businessThreadPoolSize(4)
                        .build()
                );
        coreService.registerProvider(new EchoServiceImpl(coreService), EchoService.class);
        coreService.init();
        coreService.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                coreService.stop();
            } catch (TomatoRpcException e) {
                log.error(e.getMessage(), e);
            }
        }));
    }
}
