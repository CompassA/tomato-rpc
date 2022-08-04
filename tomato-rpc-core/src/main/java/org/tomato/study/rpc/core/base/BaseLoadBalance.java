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

package org.tomato.study.rpc.core.base;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.LoadBalance;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.transport.RpcInvoker;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.01
 */
public abstract class BaseLoadBalance implements LoadBalance {

    @Override
    public RpcInvoker select(String microServiceId,
                             Invocation invocation,
                             List<RpcInvoker> invokers) throws TomatoRpcRuntimeException {
        if (CollectionUtils.isEmpty(invokers) || StringUtils.isBlank(microServiceId)) {
            return null;
        }
        // 如果只有一个invoker, 直接返回
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return doSelect(microServiceId, invocation, invokers);
    }

    protected abstract RpcInvoker doSelect(
            String microServiceId,
            Invocation invocation,
            List<RpcInvoker> invokers) throws TomatoRpcRuntimeException;
}
