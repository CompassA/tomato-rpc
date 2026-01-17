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

package org.tomato.study.rpc.config.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tomato.study.rpc.common.monitor.ApiUrl;
import org.tomato.study.rpc.config.controller.vo.InvokerMetaVO;
import org.tomato.study.rpc.config.controller.vo.InvokerStatusVO;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.dashboard.dto.RouterRefreshDTO;
import org.tomato.study.rpc.core.dashboard.dto.RpcRouterDTO;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.router.Router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 暴露一些接口供监控用
 * @author Tomato
 * Created on 2022.02.01
 */
@RestController
public record MonitorController(RpcCoreService rpcCoreService) {

    @GetMapping(ApiUrl.INVOKER_STATUS)
    public List<InvokerStatusVO> invokerStatus(
            @RequestParam("microServiceId") String microServiceId) {
        List<InvokerMetaVO> invokerMetaList = rpcCoreService.getNameServer().listInvokers(microServiceId)
                .stream()
                .map(invoker -> {
                    MetaData metadata = invoker.getMetadata();
                    InvokerMetaVO metaVO = new InvokerMetaVO();
                    metaVO.setGroup(metadata.getGroup());
                    metaVO.setHost(metadata.getHost());
                    metaVO.setPort(metadata.getPort());
                    metaVO.setStage(metadata.getStage());
                    metaVO.setProtocol(metadata.getProtocol());
                    metaVO.setMicroServiceId(metadata.getMicroServiceId());
                    metaVO.setProperty(metadata.getNodeProperty());
                    return metaVO;
                }).collect(Collectors.toList());

        List<InvokerStatusVO> result = new ArrayList<>();
        invokerMetaList.stream()
                .collect(Collectors.groupingBy(InvokerMetaVO::getStage))
                .forEach((stage, metaList) -> {
                    InvokerStatusVO statusVO = new InvokerStatusVO();
                    statusVO.setStage(stage);
                    statusVO.setGroups(
                            metaList.stream().collect(Collectors.groupingBy(InvokerMetaVO::getGroup))
                    );
                    result.add(statusVO);
                });
        return result;
    }

    @GetMapping(ApiUrl.INVOKER_READY)
    public String ready(HttpServletRequest request, HttpServletResponse response) {
        if (rpcCoreService.isReady()) {
            return "";
        }
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        return "not ready";
    }

    @GetMapping(ApiUrl.ROUTER_LIST)
    public List<String> routerList(@RequestParam("routerMicroServiceId") String routerMicroServiceId) {
        return rpcCoreService.getNameServer()
            .getMicroService(routerMicroServiceId)
            .map(MicroServiceSpace::getAllRouters)
            .orElse(Collections.emptyList())
            .stream()
            .map(Router::getExpression)
            .toList();
    }

    @PostMapping(ApiUrl.ROUTER_REFRESH)
    public String routerRefresh(@RequestBody RouterRefreshDTO request, HttpServletResponse response) {
        Optional<MicroServiceSpace> microService = rpcCoreService.getNameServer().getMicroService(request.getRouterMicroServiceId());
        if (microService.isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "unsubscribed micro service";
        }

        List<String> expressionList = request.getRpcRouterDTOList()
            .stream()
            .sorted(Comparator.comparing(RpcRouterDTO::getPriority))
            .map(RpcRouterDTO::getExpr)
            .toList();
        microService.get().refreshRouter(request.getOpsGlobalId(), expressionList);

        return "";
    }
}
