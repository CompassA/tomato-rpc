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

package org.tomato.study.rpc.dashboard.dao;

import org.tomato.study.rpc.core.dashboard.data.RpcInvokerData;
import org.tomato.study.rpc.core.dashboard.data.RpcRouterData;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
public interface NameServerDAO {

    /**
     * 查询服务在某个环境的机器数据
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @return 机器数据
     */
    List<RpcInvokerData> listInvokers(String microServiceId, String stage) throws Exception;

    /**
     * 查询服务在某个环境的路由规则数据
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @param routerMicroServiceId 路由规则路由目标的微服务ID
     * @return 路由规则数据
     */
    RpcRouterData listRouters(String microServiceId, String stage, String routerMicroServiceId) throws Exception;

    /**
     * 保存路由规则
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @param routerMicroServiceId 路由规则路由目标的微服务ID
     * @param routerData 路由规则数据
     */
    void saveRoutersAndNotify(String microServiceId, String stage, String routerMicroServiceId, RpcRouterData routerData) throws Exception;

}
