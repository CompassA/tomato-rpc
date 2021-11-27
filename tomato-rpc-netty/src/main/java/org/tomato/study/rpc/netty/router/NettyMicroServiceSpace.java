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

package org.tomato.study.rpc.netty.router;

import org.tomato.study.rpc.core.base.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.transport.RpcInvoker;
import org.tomato.study.rpc.core.transport.RpcInvokerFactory;

/**
 * 提供基于Netty创建Invoker的方法
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyMicroServiceSpace extends BaseMicroServiceSpace {

    /**
     * invoker创建
     */
    private final RpcInvokerFactory invokerFactory;

    private final long keepAliveMs;

    private final long timeoutMs;

    public NettyMicroServiceSpace(String microServiceId,
                                  RpcInvokerFactory invokerFactory,
                                  long keepAliveMs,
                                  long timeoutMs) {
        super(microServiceId);
        this.invokerFactory = invokerFactory;
        this.keepAliveMs = keepAliveMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    protected RpcInvoker createInvoker(MetaData metaData) {
        return invokerFactory.create(metaData, keepAliveMs, timeoutMs).orElse(null);
    }
}
