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
import org.tomato.study.rpc.core.invoker.RpcInvokerFactory;

/**
 * stub工厂基类
 * @author Tomato
 * Created on 2022.08.06
 */
public abstract class BaseStubFactory implements StubFactory {

    @Override
    public <T> T createStub(RpcConfig rpcConfig, StubConfig<T> config, RpcInvokerFactory invokerFactory)
            throws IllegalArgumentException {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("stub config is not valid, stub config: " + config);
        }
        StubInvoker stubInvoker = invokerFactory.createStubInvoker(config, rpcConfig).orElse(null);
        if (stubInvoker == null) {
            throw new IllegalArgumentException("createStub - create rpc stub invoker failed");
        }
        return doCreateStub(config, rpcConfig, stubInvoker);
    }

    /**
     * 创建接口代理
     * @param config stub配置
     * @param invoker stubInvoker
     * @param <T> 接口类型
     * @return 接口代理
     */
    protected abstract <T> T doCreateStub(StubConfig<T> config, RpcConfig rpcConfig, StubInvoker invoker);
}
