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

package org.tomato.study.rpc.core.stub;

import org.tomato.study.rpc.core.data.Code;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.Response;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcErrorInfo;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.registry.NameServer;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 基于配置中心动态发现
 * @author Tomato
 * Created on 2021.11.27
 */
public class RouterStubInvoker extends BaseStubInvoker {

    private final NameServer nameServer;

    public RouterStubInvoker(StubConfig<?> stubConfig) {
        super(stubConfig);
        this.nameServer = stubConfig.getNameServer();
    }

    @Override
    protected Response doInvoke(Invocation invocation) {
        try {
            Optional<RpcInvoker> invokerOpt = nameServer.lookupInvoker(invocation);
            if (invokerOpt.isEmpty()) {
                TomatoRpcErrorInfo errorInfo = TomatoRpcCoreErrorEnum.STUB_INVOKER_SEARCH_ERROR.create(
                    String.format("[invoker not found, micro-service-id=%s, interface=%s]", invocation.getMicroServiceId(), invocation.getInterfaceName()));
                throw new TomatoRpcRuntimeException(errorInfo);
            }
            Response response = invokerOpt.get().invoke(invocation).getResultSync();
            if (!Code.SUCCESS.equals(response.getCode())) {
                if (response.getData() instanceof TomatoRpcRuntimeException) {
                    throw (TomatoRpcRuntimeException) response.getData();
                } else {
                    TomatoRpcErrorInfo errorInfo = TomatoRpcCoreErrorEnum.STUB_INVOKER_RPC_ERROR.create(
                        "[rpc invocation failed, server response: " + response.getMessage() + "]");
                    throw new TomatoRpcRuntimeException(errorInfo);
                }
            }
            return response;
        } catch (ExecutionException | InterruptedException | TomatoRpcException e) {
            TomatoRpcErrorInfo errorInfo = TomatoRpcCoreErrorEnum.STUB_INVOKER_SEARCH_ERROR.create(
                String.format("[rpc invocation failed, micro-service-id=%s, interface=%s]", invocation.getMicroServiceId(), invocation.getInterfaceName()));
            throw new TomatoRpcRuntimeException(errorInfo, e);
        }
    }
}
