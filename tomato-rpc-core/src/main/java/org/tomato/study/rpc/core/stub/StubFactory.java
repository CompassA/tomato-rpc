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

import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.spi.SpiInterface;
import org.tomato.study.rpc.core.invoker.RpcInvokerFactory;

/**
 * create rpc client proxy
 * @author Tomato
 * Created on 2021.03.31
 */
@SpiInterface("jdk")
public interface StubFactory {

    /**
     * create a proxy instance which can send message to provide
     * @param rpcConfig rpc配置
     * @param config necessary data for creating a stub
     * @param invokerFactory invoker工厂
     * @param <T> proxy interface type
     * @throws IllegalArgumentException illegal argument
     * @return proxy instance
     */
    <T> T createStub(RpcConfig rpcConfig, StubConfig<T> config, RpcInvokerFactory invokerFactory)
            throws IllegalArgumentException;
}
