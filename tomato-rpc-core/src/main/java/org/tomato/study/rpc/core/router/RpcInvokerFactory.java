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

package org.tomato.study.rpc.core.router;

import org.tomato.study.rpc.core.spi.SpiInterface;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.07.11
 */
@SpiInterface(
        paramName = "rpcInvokerFactory",
        defaultSpiValue = "org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory"
)
public interface RpcInvokerFactory {

    /**
     * create rpc invoker by service provider metadata
     * @param invokerConfig necessary data for create rpc invoker
     * @return prc invoker
     */
    Optional<RpcInvoker> create(InvokerConfig invokerConfig);
}
