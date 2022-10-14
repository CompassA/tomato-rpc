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

package org.tomato.study.rpc.core.loadbalance;

import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.01
 */
public abstract class BaseLoadBalance implements LoadBalance {

    @Override
    public RpcInvoker select(Invocation invocation, List<RpcInvoker> invokers)
            throws TomatoRpcRuntimeException {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        // 如果只有一个invoker, 直接返回
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return doSelect(invocation, invokers);
    }

    protected abstract RpcInvoker doSelect(Invocation invocation, List<RpcInvoker> invokers)
            throws TomatoRpcRuntimeException;
}
