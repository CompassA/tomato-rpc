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

import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.netty.transport.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.transport.client.NettyResponseHolder;

import java.util.Optional;

/**
 * 创建一个与具体RPC节点通信的Invoker
 * @author Tomato
 * Created on 2021.07.11
 */
@Slf4j
public class NettyRpcInvokerFactory implements RpcInvokerFactory {

    private final NettyChannelHolder channelHolder;
    private final NettyResponseHolder responseHolder;

    public NettyRpcInvokerFactory(NettyChannelHolder channelHolder,
                                  NettyResponseHolder responseHolder) {
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Deprecated
    @Override
    public Optional<RpcInvoker> create(MetaData nodeInfo) {
        if (nodeInfo == null || !nodeInfo.isValid()) {
            return Optional.empty();
        }
        return Optional.of(new NettyRpcInvoker(nodeInfo, channelHolder, responseHolder));
    }
}
