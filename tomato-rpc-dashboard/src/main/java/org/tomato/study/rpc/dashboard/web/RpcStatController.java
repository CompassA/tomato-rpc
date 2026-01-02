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

package org.tomato.study.rpc.dashboard.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tomato.study.rpc.dashboard.advice.ServiceTemplate;
import org.tomato.study.rpc.dashboard.advice.ServiceTemplateEnum;
import org.tomato.study.rpc.dashboard.assembler.RpcInvokerAssembler;
import org.tomato.study.rpc.dashboard.service.ListInvokerReq;
import org.tomato.study.rpc.dashboard.service.RpcStatService;
import org.tomato.study.rpc.dashboard.service.model.RpcInvokerModel;
import org.tomato.study.rpc.dashboard.web.view.DashboardResponse;
import org.tomato.study.rpc.dashboard.web.view.RpcInvokersVO;

import java.util.List;

/**
 * rpc集群信息接口
 * @author Tomato
 * Created on 2022.08.07
 */
@RestController
@RequiredArgsConstructor
public class RpcStatController {

    private final RpcStatService rpcStatService;

    @GetMapping(ApiPath.Stat.INVOKER_LIST)
    public DashboardResponse invokerList(
            @RequestParam("microServiceId") String microServiceId,
            @RequestParam("stage") String stage) throws Exception {
        ListInvokerReq req = new ListInvokerReq();
        req.setStage(stage);
        req.setMicroServiceId(microServiceId);

        return ServiceTemplateEnum.RPC_STAT.execute("invokerList", req, new ServiceTemplate<ListInvokerReq, RpcInvokersVO>() {
            @Override
            protected RpcInvokersVO doProcess(ListInvokerReq q) throws Throwable{
                List<RpcInvokerModel> rpcInvokerModels = rpcStatService.listInvokers(q);
                return RpcInvokerAssembler.toVO(q.getMicroServiceId(), rpcInvokerModels);
            }
        });
    }
}
