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

package org.tomato.study.rpc.netty.sender;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.handler.ResponseHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * manage all connections between client and service
 * @author Tomato
 * Created on 2021.04.08
 */
public class ChannelHolder {

    public static final ChannelHolder INSTANCE = new ChannelHolder();

    /**
     * connection timeout, time unit: ms
     */
    private static final long CONNECTION_TIMEOUT = 30;

    public boolean close = false;

    /**
     * serviceURI -> service connection info
     */
    private final ConcurrentMap<URI, ChannelWrapper> channelMap;

    /**
     * client event loop
     */
    private final EventLoopGroup eventLoopGroup;

    public ChannelHolder() {
        this.channelMap = new ConcurrentHashMap<>(0);
        this.eventLoopGroup = Epoll.isAvailable() ?
                new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    /**
     * get the connection with the service provider by serviceVip,
     * if the connection is not established, create a new channel
     * @param uri service provider vip
     * @return netty channel
     * @throws Exception any exception during service discovery and connection register.
     */
    public ChannelWrapper getChannelWrapper(URI uri) throws Exception {
        ChannelWrapper channelWrapper = this.channelMap.get(uri);
        if (channelWrapper == null) {
            synchronized (ChannelHolder.class) {
                channelWrapper = this.channelMap.get(uri);
                if (channelWrapper == null) {
                    channelWrapper = this.registerChannel(uri);
                }
            }
        }
        return channelWrapper;
    }

    private ChannelWrapper registerChannel(URI serverNodeURI)
            throws InterruptedException, TimeoutException {
        Bootstrap bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .group(this.eventLoopGroup)
                .channel(Epoll.isAvailable() ?
                        EpollSocketChannel.class : NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NettyFrameEncoder())
                                .addLast(new NettyProtoDecoder())
                                .addLast(ResponseHandler.INSTANCE)
                                .addLast(new NettyFrameEncoder());
                    }
                });
        ChannelFuture connectFuture = bootstrap.connect(
                new InetSocketAddress(serverNodeURI.getHost(), serverNodeURI.getPort()));
        if (!connectFuture.await(CONNECTION_TIMEOUT, TimeUnit.SECONDS)) {
            throw new TimeoutException("netty connect timeout");
        }
        Channel channel = connectFuture.channel();
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("netty channel is not active");
        }
        ChannelWrapper channelWrapper = new ChannelWrapper(channel);
        this.channelMap.put(serverNodeURI, channelWrapper);
        return channelWrapper;
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
