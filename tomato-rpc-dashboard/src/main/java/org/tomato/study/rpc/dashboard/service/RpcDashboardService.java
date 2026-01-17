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

package org.tomato.study.rpc.dashboard.service;

import org.tomato.study.rpc.core.dashboard.dto.RpcRouterDTO;
import org.tomato.study.rpc.core.dashboard.model.RpcInvokerModel;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
public interface RpcDashboardService {

    /**
     * 展示一个服务的某个环境的所有rpc节点信息
     * @param req 请求
     * @return rpc服务节点数据
     */
    List<RpcInvokerModel> listInvokers(ListInvokerReq req) throws Exception;

    /**
     * 展示一个服务的某个环境下的路由规则
     * @param req 请求
     * @return 服务路由规则
     */
    List<RpcRouterDTO> listRouters(ListRouterReq req) throws Exception;

    /**
     * 异步更新微服务的路由规则
     * @param request 更新请求
     */
    void routerModify(RpcRouterModifyRequest request) throws Exception;
}
