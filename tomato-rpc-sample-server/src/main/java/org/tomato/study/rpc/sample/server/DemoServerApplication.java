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

import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.netty.service.NettyRpcCoreService;
import org.tomato.study.rpc.sample.api.EchoService;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

/**
 * @author Tomato
 * Created on 2021.06.20
 */
public class DemoServerApplication {

    public static void main(String[] args) throws Exception {
        String zkURL = System.getenv("ZK_IP_PORT");
        if (StringUtils.isBlank(zkURL)) {
            zkURL = "127.0.0.1:2181";
        }
        String serverVIP = "org.tomato.study.rpc.demo.server";
        int port = 1535;
        URI nameServerURI = URI.create("zookeeper://" + zkURL);

        RpcCoreService coreService = new NettyRpcCoreService(serverVIP, Collections.emptyList(), nameServerURI);
        coreService.startRpcServer(port);
        coreService.registerProvider(new EchoServiceImpl(), EchoService.class);
        System.out.println("rpc server started");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                coreService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
