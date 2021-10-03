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
import org.tomato.study.rpc.core.router.InvokerConfig;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.07.11
 */
@Slf4j
public class NettyRpcInvokerFactory implements RpcInvokerFactory {

    public static final String RESPONSE_HOLDER_PARAM_KEY = "responseHolderKey";
    public static final String CHANNEL_HOLDER_PARAM_KEY = "channelHolderKey";

    @Override
    public Optional<RpcInvoker> create(InvokerConfig invokerConfig) {
        MetaData nodeInfo = invokerConfig.getNodeInfo();
        if (!nodeInfo.isValid()) {
            return Optional.empty();
        }

        NettyChannelHolder channelHolder = invokerConfig.getParameter(CHANNEL_HOLDER_PARAM_KEY);
        if (channelHolder == null) {
            return Optional.empty();
        }

        NettyResponseHolder responseHolder = invokerConfig.getParameter(RESPONSE_HOLDER_PARAM_KEY);
        if (responseHolder == null) {
            return Optional.empty();
        }

        return Optional.of(
                new NettyRpcInvoker(nodeInfo, channelHolder, responseHolder));
    }
}
