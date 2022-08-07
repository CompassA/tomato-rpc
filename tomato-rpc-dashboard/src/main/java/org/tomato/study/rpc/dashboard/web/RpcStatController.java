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
import org.tomato.study.rpc.dashboard.service.RpcStatService;
import org.tomato.study.rpc.dashboard.web.view.AppListVO;
import org.tomato.study.rpc.dashboard.web.view.DashboardResponse;
import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;
import org.tomato.study.rpc.dashboard.web.view.RpcProvidersVO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * rpc集群信息接口
 * @author Tomato
 * Created on 2022.08.07
 */
@RestController
@RequiredArgsConstructor
public class RpcStatController {

    private final RpcStatService rpcStatService;

    /**
     * 获取服务配置列表
     * @param offset 从第几个应用开始显示
     * @param nums 要获取的服务的数量
     * @return 服务信息
     */
    @GetMapping(ApiPath.Stat.APP_LIST)
    public DashboardResponse appList(
            @RequestParam(required = false, defaultValue = "1") Integer offset,
            @RequestParam(required = false, defaultValue = "10") Integer nums) throws Exception {
        if (offset <= 0) {
            return ResponseCodeEnum.PARAMETER_INVALID.emptyBody()
                    .setMessage("offset must greater than 0");
        }
        if (nums <= 0 || nums > 20) {
            return ResponseCodeEnum.PARAMETER_INVALID.emptyBody()
                    .setMessage("nums must greater than 0 and less than 20");
        }
        List<AppListVO.AppConfigInfo> apps = rpcStatService.showRpcModels(offset, nums).stream()
                .map(rpcAppModel -> new AppListVO.AppConfigInfo(rpcAppModel.getAppName()))
                .collect(Collectors.toList());
        return ResponseCodeEnum.SUCCESS.withBody(new AppListVO(apps));
    }

    @GetMapping(ApiPath.Stat.NODE_LIST)
    public DashboardResponse nodeList(
            @RequestParam("service-id") String serviceId,
            @RequestParam(required = false, defaultValue = "dev") String stage) throws Exception {
        Optional<RpcProvidersVO> rpcProvidersVO = RpcProvidersVO.toVO(
                rpcStatService.listProviders(serviceId, stage));
        return rpcProvidersVO.map(ResponseCodeEnum.SUCCESS::withBody)
                .orElseGet(ResponseCodeEnum.RPC_SERVICE_NOT_FOUND::emptyBody);
    }
}
