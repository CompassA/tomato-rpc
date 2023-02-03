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

import org.tomato.study.rpc.dashboard.dao.data.RpcAppData;
import org.tomato.study.rpc.dashboard.dao.data.RpcAppProviderData;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
public interface NameServerDAO {

    /**
     * 列举rpc应用信息
     * @param start 从第几个应用开始列举
     * @param nums 要列举多少应用
     * @return 应用数据
     */
    List<RpcAppData> listRpcData(int start, int nums) throws Exception;

    /**
     * 查询应用在某个环境的机器数据
     * @param appName app应用名
     * @param stage app环境
     * @return 机器数据
     */
    List<RpcAppProviderData> listProviders(String appName, String stage) throws Exception;
}
