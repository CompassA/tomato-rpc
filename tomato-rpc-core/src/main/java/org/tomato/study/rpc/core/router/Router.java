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

package org.tomato.study.rpc.core.router;

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

/**
 * 路由规则
 * @author Tomato
 * Created on 2023.02.12
 */
public interface Router {

    /**
     * 判断RPC请求是否命中路由规则
     * @param invocation RPC请求
     * @return true 命中规则
     */
    boolean matchRequest(Invocation invocation);

    /**
     * 判断RPC invoker是否满足条件
     * @param rpcInvoker invoker
     * @return true 满足条件
     */
    boolean matchInvoker(RpcInvoker rpcInvoker);
}
