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

package org.tomato.study.rpc.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * manage all connections between client and service
 * @author Tomato
 * Created on 2021.04.08
 */
public class NettyChannelHolder {

    private static final String RPC_CLIENT_THREAD_NAME = "rpc-client-worker-thread";

    /**
     * connection timeout, time unit: ms
     */
    private static final long CONNECTION_TIMEOUT = 10000;

    /**
     * is channel holder closed
     */
    public boolean close = false;

    /**
     * serviceURI -> service connection info
     */
    private final ConcurrentMap<URI, ChannelWrapper> channelMap;

    /**
     * client event loop
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * netty client bootstrap
     */
    private final Bootstrap bootstrap;

    public NettyChannelHolder(List<ChannelHandler> responseHandlers) {
        if (CollectionUtils.isEmpty(responseHandlers)) {
            throw new TomatoRpcRuntimeException(
                    NettyRpcErrorEnum.LIFE_CYCLE_START_ERROR.create("without response handler list"));
        }
        this.channelMap = new ConcurrentHashMap<>(0);
        this.eventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(new DefaultThreadFactory(RPC_CLIENT_THREAD_NAME))
                : new NioEventLoopGroup(new DefaultThreadFactory(RPC_CLIENT_THREAD_NAME));
        this.bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .group(this.eventLoopGroup)
                .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        channelPipeline.addLast("frame-decoder", new NettyFrameDecoder());
                        channelPipeline.addLast("proto-decoder", new NettyProtoDecoder());
                        channelPipeline.addLast("frame-encoder", new NettyFrameEncoder());
                        for (ChannelHandler responseHandler : responseHandlers) {
                            channelPipeline.addLast(
                                    responseHandler.getClass().getSimpleName(),
                                    responseHandler
                            );
                        }
                    }
                });

    }

    /**
     * get the connection with the service provider by serviceVip,
     * if the connection is not established, create a new channel
     * @param uri service provider vip
     * @return netty channel
     * @throws Exception any exception during service discovery and connection register.
     */
    public ChannelWrapper getChannelWrapper(URI uri) throws Exception {
        ChannelWrapper channelWrapper = channelMap.get(uri);
        if (channelWrapper == null) {
            synchronized (NettyChannelHolder.class) {
                channelWrapper = channelMap.get(uri);
                if (channelWrapper == null || !channelWrapper.isActiveChannel()) {
                    channelWrapper = createChannel(uri);
                    channelMap.put(uri, channelWrapper);
                }
            }
        }
        return channelWrapper;
    }

    public void removeChannel(URI uri) {
        ChannelWrapper channelWrapper = channelMap.get(uri);
        if (channelWrapper != null) {
            channelWrapper.closeChannel();
        }
    }

    private ChannelWrapper createChannel(URI serverNodeURI)
            throws InterruptedException, TimeoutException {
        ChannelFuture connectFuture = bootstrap.connect(
                serverNodeURI.getHost(),
                serverNodeURI.getPort()
        );
        if (!connectFuture.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("netty connect timeout");
        }
        Channel channel = connectFuture.channel();
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("netty channel is not active");
        }
        return new ChannelWrapper(channel);
    }

    public synchronized void close() {
        if (this.close) {
            return;
        }
        this.close = true;
        for (ChannelWrapper channelWrapper : this.channelMap.values()) {
            channelWrapper.closeChannel();
        }
        this.channelMap.clear();
        this.eventLoopGroup.shutdownGracefully();
    }
}
