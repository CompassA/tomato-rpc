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

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.01
 */
public class RandomLoadBalance extends BaseLoadBalance {

    @Override
    protected RpcInvoker doSelect(Invocation invocation, List<RpcInvoker> invokers)
            throws TomatoRpcRuntimeException {
        // 选择合法节点的第n个invoker, n为随机数
        int randomTarget = (int) (Math.random() * invokers.size()) + 1;
        int curValidCnt = 0;
        for (RpcInvoker invoker : invokers) {
            if (invoker.isUsable()) {
                ++curValidCnt;
            }
            if (curValidCnt == randomTarget) {
                return invoker;
            }
        }
        return null;
    }
}
