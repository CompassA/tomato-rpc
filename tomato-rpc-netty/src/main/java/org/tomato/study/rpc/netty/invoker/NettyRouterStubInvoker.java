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

package org.tomato.study.rpc.netty.invoker;

import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.data.Code;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;

import java.util.concurrent.ExecutionException;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
public class NettyRouterStubInvoker extends NettyBaseStubInvoker {

    private final NameServer nameServer;

    public NettyRouterStubInvoker(String microServiceId,
                                  String group,
                                  Class<?> serviceInterface,
                                  NameServer nameServer) {
        super(microServiceId, group, serviceInterface);
        this.nameServer = nameServer;
    }

    @Override
    protected Response doInvoke(Invocation invocation) {
        String microServiceId = getMicroServiceId();
        String group = getGroup();
        try {
            Response response = nameServer.lookupInvoker(microServiceId, group)
                    .orElseThrow(() -> new TomatoRpcRuntimeException(
                            NettyRpcErrorEnum.STUB_INVOKER_SEARCH_ERROR.create(
                                    String.format("[invoker not found, micro-service-id=%s, group=%s",
                                            microServiceId, group))))
                    .invoke(invocation)
                    .getResultSync();
            if (!Code.SUCCESS.equals(response.getCode())) {
                throw new TomatoRpcRuntimeException(
                        NettyRpcErrorEnum.STUB_INVOKER_RPC_ERROR.create(
                                "[rpc invocation failed, server response: " + response.getMessage() + "]"));
            }
            return response;
        } catch (ExecutionException | InterruptedException | TomatoRpcException e) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.STUB_INVOKER_SEARCH_ERROR.create(
                    String.format("[rpc invocation failed, micro-service-id=%s, group=%s", microServiceId, group)), e);
        }
    }
}
