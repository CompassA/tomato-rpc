/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty.invoker;

import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.invoker.BaseRpcInvokerFactory;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.Optional;

/**
 * 创建一个与具体RPC节点通信的Invoker
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvokerFactory extends BaseRpcInvokerFactory {

    @Override
    public Optional<RpcInvoker> create(MetaData nodeInfo, RpcConfig rpcConfig) {
        if (nodeInfo == null) {
            return Optional.empty();
        }
        return Optional.of(new NettyRpcInvoker(nodeInfo, rpcConfig));
    }
}
