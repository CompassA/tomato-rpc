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

package org.tomato.study.rpc.sample.spring.server.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.config.annotation.RpcClientStub;
import org.tomato.study.rpc.config.annotation.RpcServerStub;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.sample.api.ExecutionChainFacade;
import org.tomato.study.rpc.sample.api.data.Constant;
import org.tomato.study.rpc.sample.api.data.ExecutionChainRequest;
import org.tomato.study.rpc.sample.api.data.ExecutionChainResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Tomato
 * Created on 2026.01.05
 */
@Slf4j
@RpcServerStub
public class ExecutionChainFacadeImpl implements ExecutionChainFacade {

    @Resource
    private RpcCoreService rpcCoreService;

    @RpcClientStub(microServiceId = Constant.testDownStreamServiceId)
    private ExecutionChainFacade downstreamFacade;

    @Override
    public ExecutionChainResponse dispatch(ExecutionChainRequest request) {
        log.info("receive dispatch");
        List<String> serverURLList = new ArrayList<>(request.getServerURLList());
        MetaData rpcServerMetaData = rpcCoreService.getRpcServerMetaData();
        serverURLList.add(MetaData.convert(rpcServerMetaData).get().toASCIIString());

        if (!Objects.equals(rpcServerMetaData.getMicroServiceId(), Constant.testDownStreamServiceId)) {
            ExecutionChainRequest downstreamRequest = new ExecutionChainRequest();
            downstreamRequest.setServerURLList(serverURLList);
            return downstreamFacade.dispatch(downstreamRequest);
        }


        ExecutionChainResponse response = new ExecutionChainResponse();
        response.setServerURLList(serverURLList);
        return response;
    }
}
