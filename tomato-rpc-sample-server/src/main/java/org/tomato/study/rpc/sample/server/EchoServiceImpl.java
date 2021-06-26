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

import org.tomato.study.rpc.netty.utils.NetworkUtil;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.DemoResponse;

import java.net.InetAddress;

/**
 * @author Tomato
 * Created on 2021.06.20
 */
public class EchoServiceImpl implements EchoService {

    @Override
    public DemoResponse echo(DemoRequest request) {
        DemoResponse response = new DemoResponse();
        StringBuilder builder = new StringBuilder();
        try {
            InetAddress localAddress = NetworkUtil.getLocalAddress();
            if (localAddress == null) {
                throw new RuntimeException("local address == null");
            }
            builder.append("hello client!\n")
                    .append("request message: ")
                    .append(request.getData())
                    .append("\n")
                    .append("host address: ")
                    .append(localAddress.getHostAddress())
                    .append("\n")
                    .append("host name: ")
                    .append(localAddress.getHostName())
                    .append("\n");
        } catch (Exception e) {
            e.printStackTrace();
            response.setData(e.getMessage());
            return response;

        }
        response.setData(builder.toString());
        return response;
    }
}
