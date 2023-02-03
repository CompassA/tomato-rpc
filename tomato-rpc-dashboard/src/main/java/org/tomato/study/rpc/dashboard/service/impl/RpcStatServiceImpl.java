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

package org.tomato.study.rpc.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.tomato.study.rpc.dashboard.dao.NameServerManager;
import org.tomato.study.rpc.dashboard.dao.data.RpcAppData;
import org.tomato.study.rpc.dashboard.dao.data.RpcAppProviderData;
import org.tomato.study.rpc.dashboard.service.RpcStatService;
import org.tomato.study.rpc.dashboard.service.model.RpcAppModel;
import org.tomato.study.rpc.dashboard.service.model.RpcProviderModel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@Service
@RequiredArgsConstructor
public class RpcStatServiceImpl implements RpcStatService {

    private final NameServerManager nameServerDAO;

    @Override
    public List<RpcAppModel> showRpcModels(int offset, int numbers) throws Exception {
        List<RpcAppData> rpcAppData = nameServerDAO.listRpcData(offset, numbers);
        if (CollectionUtils.isEmpty(rpcAppData)) {
            return Collections.emptyList();
        }
        return rpcAppData.stream()
                .map(data -> new RpcAppModel(data.getAppName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<RpcProviderModel> listProviders(String appName, String stage) throws Exception {
        List<RpcAppProviderData> providers = nameServerDAO.listProviders(appName, stage);
        if (providers == null) {
            return Collections.emptyList();
        }
        return providers.stream()
                .map(data -> new RpcProviderModel(data.getAppName(), data.getNodeProperties(), data.getRpcConfig()))
                .collect(Collectors.toList());

    }
}
