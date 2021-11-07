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

import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.router.ServiceProviderFactory;
import org.tomato.study.rpc.netty.transport.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.transport.client.NettyResponseHolder;

/**
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyServiceProviderFactory implements ServiceProviderFactory {

    private final NettyChannelHolder channelHolder;

    private final NettyResponseHolder responseHolder;

    public NettyServiceProviderFactory(NettyChannelHolder channelHolder,
                                       NettyResponseHolder responseHolder) {
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    public MicroServiceSpace create(String vip) {
        return new NettyMicroServiceSpace(vip, channelHolder, responseHolder);
    }
}
