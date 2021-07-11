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
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.netty.service.NettyRpcCoreService;
import org.tomato.study.rpc.netty.service.RpcConfig;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.DemoResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Tomato
 * Created on 2021.06.21
 */
public class DemoClientApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        String zkURL = System.getenv("ZK_IP_PORT");
        if (StringUtils.isBlank(zkURL)) {
            zkURL = "127.0.0.1:2181";
        }
        RpcConfig rpcConfig = RpcConfig.builder()
                .serviceVIP("org.tomato.study.rpc.demo.client")
                .subscribedVIP(Collections.singletonList("org.tomato.study.rpc.demo.server"))
                .nameServiceURI(zkURL)
                .build();
        RpcCoreService rpcCoreService = new NettyRpcCoreService(rpcConfig);
        EchoService stub = rpcCoreService.createStub(
                rpcConfig.getSubscribedVIP().get(0), EchoService.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpcCoreService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        // call RPC method 3 seconds a time
        while (true) {
            DemoResponse response = stub.echo(new DemoRequest("hello world"));
            System.out.println(response.getData());
            Thread.sleep(3000);
        }
    }
}
