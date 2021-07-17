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

import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.data.NettyInvocationResult;

import java.io.IOException;

/**
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvoker implements RpcInvoker {

    private final MetaData providerNodeMetaData;

    private final Serializer commandSerializer = SpiLoader.getLoader(Serializer.class).load();

    private final MessageSender sender;

    public NettyRpcInvoker(MetaData providerNodeMetaData) throws Exception {
        this.providerNodeMetaData = providerNodeMetaData;
        this.sender = SpiLoader.getLoader(SenderFactory.class)
                .load()
                .create(providerNodeMetaData.getHost(), providerNodeMetaData.getPort());
    }

    @Override
    public String getVersion() {
        return providerNodeMetaData.getVersion();
    }

    @Override
    public MetaData getMetadata() {
        return providerNodeMetaData;
    }

    @Override
    public Result<Response> invoke(Invocation invocation) {
        return new NettyInvocationResult(
                sender.send(
                        CommandFactory.INSTANCE.request(
                                invocation, commandSerializer, CommandType.RPC_REQUEST))
        );
    }

    @Override
    public void close() throws IOException {
        this.sender.close();
    }
}
