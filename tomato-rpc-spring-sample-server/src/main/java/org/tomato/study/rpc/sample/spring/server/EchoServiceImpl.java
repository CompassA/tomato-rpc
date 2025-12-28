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

package org.tomato.study.rpc.sample.spring.server;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.config.annotation.RpcServerStub;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.sample.api.EchoService;
import org.tomato.study.rpc.sample.api.SumService;
import org.tomato.study.rpc.sample.api.data.DemoRequest;
import org.tomato.study.rpc.sample.api.data.DemoResponse;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomato
 * Created on 2021.11.20
 */
@Slf4j
@RpcServerStub
@AllArgsConstructor
public class EchoServiceImpl implements EchoService, SumService {

    private final RpcCoreService coreService;
    private final AtomicLong cnt = new AtomicLong(0);

    @Override
    public DemoResponse echo(DemoRequest request) {
        DemoResponse response = new DemoResponse();
        StringBuilder builder = new StringBuilder();
        try {
            InetAddress localAddress = NetworkUtil.getLocalAddress();
            if (localAddress == null) {
                throw new RuntimeException("local address == null");
            }
            builder.append("host=").append(localAddress.getHostAddress()).append("|")
                .append("port=").append(coreService.getPort()).append("|")
                .append("micro-service-id=").append(coreService.getMicroServiceId()).append("|")
                .append("no.").append(cnt.addAndGet(1)).append("|")
                .append("stage=").append(coreService.getStage()).append("|")
                .append("group=").append(coreService.getGroup()).append("|")
                .append("message=").append(request.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setData(e.getMessage());
            return response;
        }
        response.setData(builder.toString());
        return response;
    }

    @Override
    public Integer sum(List<Integer> nums) {
        if (CollectionUtils.isEmpty(nums)) {
            return 0;
        }
        // 测试超时
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
        return nums.stream().reduce(0, Integer::sum);
    }
}
