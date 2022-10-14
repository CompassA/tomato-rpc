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

import org.tomato.study.rpc.dashboard.service.model.RpcAppModel;
import org.tomato.study.rpc.dashboard.service.model.RpcProviderModel;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
public interface RpcStatService {

    /**
     * 展示多个rpc应用信息信息
     * @param offset 从第几个app开始展示, 初始值1
     * @param numbers 要展示的应用的数量
     * @return rpc应用信息
     */
    List<RpcAppModel> showRpcModels(int offset, int numbers) throws Exception;

    /**
     * 展示一个应用的某个环境的所有rpc节点信息
     * @param appName app名称
     * @param stage 环境筛选
     * @return rpc服务节点数据
     */
    List<RpcProviderModel> listProviders(String appName, String stage) throws Exception;
}
