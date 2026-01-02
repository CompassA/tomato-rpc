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
import org.springframework.stereotype.Service;
import org.tomato.study.rpc.dashboard.dao.NameServerManager;
import org.tomato.study.rpc.dashboard.dao.data.RpcInvokerData;
import org.tomato.study.rpc.dashboard.service.ListInvokerReq;
import org.tomato.study.rpc.dashboard.service.RpcStatService;
import org.tomato.study.rpc.dashboard.service.model.RpcInvokerModel;

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
    public List<RpcInvokerModel> listInvokers(ListInvokerReq req) throws Exception {
        List<RpcInvokerData> invokers = nameServerDAO.listInvokers(req.getMicroServiceId(), req.getStage());
        if (invokers == null) {
            return Collections.emptyList();
        }
        return invokers.stream()
                .map(data -> new RpcInvokerModel(data.getMicroServiceId(), data.getNodeProperties()))
                .collect(Collectors.toList());

    }
}
