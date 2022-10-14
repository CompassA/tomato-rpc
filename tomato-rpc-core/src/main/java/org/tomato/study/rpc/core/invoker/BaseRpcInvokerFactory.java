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

package org.tomato.study.rpc.core.invoker;

import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.stub.DirectStubInvoker;
import org.tomato.study.rpc.core.stub.RouterStubInvoker;
import org.tomato.study.rpc.core.stub.StubInvoker;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2022.08.06
 */
public abstract class BaseRpcInvokerFactory implements RpcInvokerFactory {

    @Override
    public Optional<StubInvoker> createStubInvoker(StubConfig<?> stubConfig, RpcConfig rpcConfig) {
        if (stubConfig == null) {
            return Optional.empty();
        }
        // 有配置中心, 创建路由invoker
        if (stubConfig.getNameServer() != null) {
            return Optional.of(new RouterStubInvoker(stubConfig));
        }
        // 无配置中心, 创建直连invoker
        MetaData nodeInfo = stubConfig.getNodeInfo();
        if (nodeInfo != null && nodeInfo.isValid()) {
            return create(nodeInfo, rpcConfig)
                    .map(rpcInvoker -> new DirectStubInvoker(stubConfig, rpcInvoker));
        }
        return Optional.empty();
    }
}
