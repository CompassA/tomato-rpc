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
import org.tomato.study.rpc.core.spi.SpiInterface;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.01
 */
@SpiInterface("weight-round-robin")
public interface LoadBalance {

    /**
     * 挑选一个invoker进行调用
     * @param invocation rpc请求
     * @param invokers 候选invoker
     * @throws TomatoRpcRuntimeException 均衡负责异常
     * @return 选中的invoker
     */
    RpcInvoker select(Invocation invocation, List<RpcInvoker> invokers)
            throws TomatoRpcRuntimeException;
}
