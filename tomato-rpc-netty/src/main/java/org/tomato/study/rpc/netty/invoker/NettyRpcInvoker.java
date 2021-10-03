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

import io.netty.channel.ChannelFutureListener;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.data.NettyInvocationResult;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * RPC客户端调用者，负责与一个RPC服务的某个具体节点通信
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvoker implements RpcInvoker, MessageSender {

    /**
     * RPC服务节点的ip、端口等数据
     */
    private final MetaData providerNodeMetaData;

    /**
     * 客户端的连接管理器
     */
    private final NettyChannelHolder channelHolder;

    /**
     * 客户端的响应管理器
     */
    private final NettyResponseHolder responseHolder;

    /**
     * 客户端为请求body体设置的序列化方式
     */
    private final Serializer commandSerializer;

    /**
     * RPC服务节点的ip、端口，URI形式
     */
    private final URI uri;

    public NettyRpcInvoker(MetaData providerNodeMetaData,
                           NettyChannelHolder channelHolder,
                           NettyResponseHolder responseHolder) {
        this.providerNodeMetaData = providerNodeMetaData;
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
        this.commandSerializer = SpiLoader.getLoader(Serializer.class).load();
        this.uri = URI.create("tomato://" + providerNodeMetaData.getHost() + ":" + providerNodeMetaData.getPort());
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
                send(CommandFactory.request(invocation, commandSerializer, CommandType.RPC_REQUEST))
        );
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        long id = msg.getHeader().getId();
        responseHolder.putFeatureResponse(id, future);

        try {
            channelHolder.getChannelWrapper(uri)
                    .getChannel()
                    .writeAndFlush(msg)
                    .addListener((ChannelFutureListener) futureChannel -> {
                        if (!futureChannel.isSuccess()) {
                            future.completeExceptionally(
                                    new TomatoRpcRuntimeException(
                                            NettyRpcErrorEnum.STUB_INVOKER_RPC_ERROR.create(),
                                            futureChannel.cause()
                                    )
                            );
                            responseHolder.getAndRemove(id);
                        }
                    });
        } catch (Exception e) {
            responseHolder.getAndRemove(id);
            throw new TomatoRpcRuntimeException(
                    NettyRpcErrorEnum.STUB_INVOKER_RPC_ERROR.create("channel fetch error"), e);
        }
        return future;
    }

    @Override
    public String getHost() {
        return uri.getHost();
    }

    @Override
    public int getPort() {
        return uri.getPort();
    }

    @Override
    public void close() {
        channelHolder.removeChannel(uri);
    }
}
