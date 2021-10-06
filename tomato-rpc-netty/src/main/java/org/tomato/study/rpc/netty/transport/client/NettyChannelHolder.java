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

package org.tomato.study.rpc.netty.transport.client;

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
import org.tomato.study.rpc.netty.codec.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.NettyProtoDecoder;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.transport.handler.ClientIdleCheckHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 管理客户端的所有连接
 * @author Tomato
 * Created on 2021.04.08
 */
public class NettyChannelHolder {

    private static final String RPC_CLIENT_THREAD_NAME = "rpc-client-worker-thread";

    /**
     * 连接超时时间
     */
    private static final long CONNECTION_TIMEOUT = 10000;

    /**
     * 关闭标志位
     */
    public boolean close = false;

    /**
     * 服务节点URI -> 连接数据
     */
    private final ConcurrentMap<URI, ChannelWrapper> channelMap;

    /**
     * 客户端循环
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * 客户端启动类
     */
    private final Bootstrap bootstrap;

    public NettyChannelHolder(long keepAliveMilliseconds, List<ChannelHandler> responseHandlers) {
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
                        channelPipeline.addLast("client-idle-checker", new ClientIdleCheckHandler(keepAliveMilliseconds));
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
     * 根据服务节点URI得到连接, 若连接未建立, 先建立连接
     * @param uri 一个服务节点的IP、端口等信息
     * @return 连接包装类
     * @throws InterruptedException 创建连接等待过程中线程被中断
     * @throws TimeoutException 创建连接等待超时
     */
    public ChannelWrapper getOrCreateChannelWrapper(URI uri) throws InterruptedException, TimeoutException {
        ChannelWrapper channelWrapper = channelMap.get(uri);
        if (channelWrapper == null) {
            synchronized (NettyChannelHolder.class) {
                channelWrapper = channelMap.get(uri);
                // 当未连接或者连接已经被关闭了，重新建立连接
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
                serverNodeURI.getPort());
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
