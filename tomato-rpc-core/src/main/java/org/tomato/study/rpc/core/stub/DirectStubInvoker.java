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

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.Response;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.concurrent.ExecutionException;

/**
 * 直连调用
 * @author Tomato
 * Created on 2021.11.27
 */
public class DirectStubInvoker extends BaseStubInvoker {

    private final RpcInvoker rpcInvoker;

    public DirectStubInvoker(StubConfig<?> stubConfig, RpcInvoker rpcInvoker) {
        super(stubConfig);
        this.rpcInvoker = rpcInvoker;
    }

    @Override
    protected Response doInvoke(Invocation invocation) {
        try {
            Response response = rpcInvoker.invoke(invocation).getResultSync();
            if (TomatoRpcErrorEnum.SUCCESS.getCode() != response.getCode()) {
                throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.valueOfCode(response.getCode()),
                    String.format("rpc invocation failed, errCode:%d, errMsg:%s", response.getCode(), response.getMessage()));
            }
            return response;
        } catch (ExecutionException | InterruptedException | TomatoRpcException e) {
            throw new TomatoRpcRuntimeException(e, TomatoRpcErrorEnum.STUB_INVOKER_SEARCH_ERROR,
                String.format("rpc invocation failed, micro-service-id=%s, invoker meta=%s", getMicroServiceId(), rpcInvoker.getMetadata()));
        }
    }
}
