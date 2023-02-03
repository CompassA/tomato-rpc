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

import org.tomato.study.rpc.core.stub.StubInvoker;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.spi.SpiInterface;

import java.util.Optional;

/**
 * 创建Invoker
 * @author Tomato
 * Created on 2021.07.11
 */
@SpiInterface("netty")
public interface RpcInvokerFactory {

    /**
     * create rpc invoker by service provider metadata
     * keep-alive and wait-timeout are default
     * @param nodeInfo necessary data for create rpc invoker
     * @return RPC invoker
     */
    Optional<RpcInvoker> create(MetaData nodeInfo, RpcConfig rpcConfig);

    /**
     * 创建stub invoker
     * @return stub invoker
     */
    Optional<StubInvoker> createStubInvoker(StubConfig<?> stubConfig, RpcConfig rpcConfig);
}
