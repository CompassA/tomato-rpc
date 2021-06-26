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
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.DemoResponse;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
        String vip = "org.tomato.study.rpc.demo.client";
        List<String> subscribedVIP = new ArrayList<>();
        subscribedVIP.add("org.tomato.study.rpc.demo.server");
        URI nameServerURI = URI.create("zookeeper://" + zkURL);
        RpcCoreService rpcCoreService = new NettyRpcCoreService(vip, subscribedVIP, nameServerURI);
        EchoService stub = rpcCoreService.createStub(subscribedVIP.get(0), EchoService.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpcCoreService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        while (true) {
            DemoResponse response = stub.echo(new DemoRequest("hello world"));
            System.out.println(response.getData());
            Thread.sleep(3000);
        }
    }
}
