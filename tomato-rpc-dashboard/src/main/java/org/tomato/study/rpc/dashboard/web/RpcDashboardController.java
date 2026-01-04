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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tomato.study.rpc.core.dashboard.dto.RpcRouterDTO;
import org.tomato.study.rpc.core.dashboard.model.RpcInvokerModel;
import org.tomato.study.rpc.dashboard.advice.ServiceTemplate;
import org.tomato.study.rpc.dashboard.advice.ServiceTemplateEnum;
import org.tomato.study.rpc.dashboard.assembler.RpcInvokerAssembler;
import org.tomato.study.rpc.dashboard.service.ListInvokerReq;
import org.tomato.study.rpc.dashboard.service.ListRouterReq;
import org.tomato.study.rpc.dashboard.service.RpcDashboardService;
import org.tomato.study.rpc.dashboard.service.RpcRouterModifyRequest;
import org.tomato.study.rpc.dashboard.web.view.DashboardResponse;
import org.tomato.study.rpc.dashboard.web.view.RpcInvokersVO;

import java.util.List;

/**
 * rpc集群信息接口
 * @author Tomato
 * Created on 2022.08.07
 */
@RestController
public record RpcDashboardController(RpcDashboardService rpcDashboardService) {

    @GetMapping(ApiPath.Stat.INVOKER_LIST)
    public DashboardResponse invokerList(@RequestParam("microServiceId") String microServiceId,
                                         @RequestParam("stage") String stage) {
        ListInvokerReq req = new ListInvokerReq();
        req.setStage(stage);
        req.setMicroServiceId(microServiceId);

        return ServiceTemplateEnum.RPC_STAT.execute("invokerList", req, new ServiceTemplate<>() {
            @Override
            protected RpcInvokersVO doProcess(ListInvokerReq q) throws Throwable{
                List<RpcInvokerModel> rpcInvokerModels = rpcDashboardService.listInvokers(q);
                return RpcInvokerAssembler.toVO(q.getMicroServiceId(), rpcInvokerModels);
            }
        });
    }

    @GetMapping(ApiPath.Stat.ROUTER_LIST)
    public DashboardResponse routerList(@RequestParam("microServiceId") String microServiceId,
                                        @RequestParam("stage") String stage,
                                        @RequestParam("routerMicroServiceId") String routerMicroServiceId) {
        ListRouterReq req = new ListRouterReq();
        req.setMicroServiceId(microServiceId);
        req.setStage(stage);
        req.setRouterMicroServiceId(routerMicroServiceId);

        return ServiceTemplateEnum.RPC_STAT.execute("routerList", req, new ServiceTemplate<>() {
            @Override
            protected List<RpcRouterDTO> doProcess(ListRouterReq listRouterReq) throws Throwable {
                return rpcDashboardService.listRouters(listRouterReq);
            }
        });
    }

    @PostMapping(ApiPath.Ops.ROUTER_MODIFY)
    public DashboardResponse routerModify(@RequestBody RpcRouterModifyRequest request) {
        return ServiceTemplateEnum.RPC_OPS.execute("routerModify", request, new ServiceTemplate<>() {

            @Override
            protected Object doProcess(RpcRouterModifyRequest rpcRouterModifyRequest) throws Throwable {
                rpcDashboardService.routerModify(rpcRouterModifyRequest);
                return null;
            }
        });
    }

}
