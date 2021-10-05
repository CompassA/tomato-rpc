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

import org.tomato.study.rpc.core.base.BaseServiceProvider;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.InvokerConfig;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;

import static org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory.CHANNEL_HOLDER_PARAM_KEY;
import static org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory.RESPONSE_HOLDER_PARAM_KEY;

/**
 * 提供基于Netty创建Invoker的方法
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyServiceProvider extends BaseServiceProvider {

    /**
     * invoker创建
     */
    private final RpcInvokerFactory nettyRpcInvokerFactory = new NettyRpcInvokerFactory();

    /**
     * 连接管理
     */
    private final NettyChannelHolder channelHolder;

    /**
     * 响应管理
     */
    private final NettyResponseHolder responseHolder;

    public NettyServiceProvider(String vip,
                                NettyChannelHolder channelHolder,
                                NettyResponseHolder responseHolder) {
        super(vip);
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    protected RpcInvoker createInvoker(MetaData metaData) {
        return nettyRpcInvokerFactory.create(
                InvokerConfig.builder()
                        .nodeInfo(metaData)
                        .parameter(CHANNEL_HOLDER_PARAM_KEY, channelHolder)
                        .parameter(RESPONSE_HOLDER_PARAM_KEY, responseHolder)
                        .build()
        ).orElse(null);
    }
}
