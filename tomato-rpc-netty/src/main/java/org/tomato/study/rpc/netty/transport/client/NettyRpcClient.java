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

package org.tomato.study.rpc.netty.transport.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import lombok.AllArgsConstructor;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.ResponseFuture;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.transport.BaseRpcClient;
import org.tomato.study.rpc.netty.codec.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.NettyProtoDecoder;
import org.tomato.study.rpc.netty.transport.handler.ClientIdleCheckHandler;
import org.tomato.study.rpc.netty.transport.handler.KeepAliveHandler;
import org.tomato.study.rpc.netty.transport.handler.ResponseHandler;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
public class NettyRpcClient extends BaseRpcClient<Command> {

    private static final String RPC_CLIENT_THREAD_NAME = "rpc-client-worker-thread";
    private static final long CONNECTION_TIMEOUT_MS = 10000;

    /**
     * 客户端的响应管理器
     */
    private final NettyResponseHolder responseHolder;

    /**
     * 客户端循环
     */
    private EventLoopGroup eventLoopGroup;

    /**
     * 客户端启动类
     */
    private Bootstrap bootstrap;

    /**
     * 心跳包发送时间间隔
     */
    private final long keepAliveMs;

    /**
     * 与服务端的连接
     */
    private ChannelWrapper channelWrapper;

    public NettyRpcClient(URI uri, long keepAliveMs) {
        super(uri);
        this.keepAliveMs = keepAliveMs;
        this.responseHolder = new NettyResponseHolder();
        try {
            init();
            start();
        } catch (TomatoRpcException e) {
            Logger.DEFAULT.error("start netty rpc client error", e);
        }
    }

    @Override
    public ResponseFuture<Command> send(Command msg) throws TomatoRpcException {
        try {
            long id = msg.getHeader().getId();
            CompletableFuture<Command> future = new CompletableFuture<>();
            // get connection
            Channel connection = createOrReconnect().getChannel();

            // write message
            connection.writeAndFlush(msg)
                    .addListener((ChannelFutureListener) futureChannel -> {
                        if (futureChannel.isSuccess()) {
                            responseHolder.putFeatureResponse(id, future);
                        } else {
                            future.completeExceptionally(
                                    new TomatoRpcRuntimeException(futureChannel.cause(), TomatoRpcErrorEnum.NETTY_CLIENT_RPC_ERROR));
                        }
                    });
            return new ClientResponseFuture(id, future, responseHolder);
        } catch (Exception e) {
            throw new TomatoRpcException(e, TomatoRpcErrorEnum.NETTY_CLIENT_RPC_ERROR,
                String.format("channel[%s,%d] fetch error", getHost(), getPort()));
        }
    }

    @Override
    public boolean isUsable() {
        return START_FINISHED == getState();
    }

    @Override
    protected void doInit() {
        this.eventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(new DefaultThreadFactory(RPC_CLIENT_THREAD_NAME))
                : new NioEventLoopGroup(new DefaultThreadFactory(RPC_CLIENT_THREAD_NAME));
        KeepAliveHandler keepAliveHandler = new KeepAliveHandler();
        ResponseHandler responseHandler = new ResponseHandler(responseHolder);
        this.bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .group(this.eventLoopGroup)
                .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        channelPipeline.addLast("client-idle-checker", new ClientIdleCheckHandler(keepAliveMs));
                        channelPipeline.addLast("frame-decoder", new NettyFrameDecoder());
                        channelPipeline.addLast("proto-decoder", new NettyProtoDecoder());
                        channelPipeline.addLast("frame-encoder", new NettyFrameEncoder());
                        channelPipeline.addLast("keep-alive-handler", keepAliveHandler);
                        channelPipeline.addLast("response-handler", responseHandler);
                    }
                });
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        if (channelWrapper != null && channelWrapper.isActiveChannel()) {
            channelWrapper.closeChannel();
        }
        this.eventLoopGroup.shutdownGracefully();
    }

    /**
     * 根据服务节点URI得到连接, 若连接未建立, 先建立连接
     * @return 连接包装类
     * @throws InterruptedException 创建连接等待过程中线程被中断
     * @throws TimeoutException 创建连接等待超时
     */
    public ChannelWrapper createOrReconnect() throws InterruptedException, TimeoutException {
        if (channelWrapper == null || !channelWrapper.isActiveChannel()) {
            synchronized (this) {
                // 当未连接或者连接已经被关闭了，重新建立连接
                if (channelWrapper == null || !channelWrapper.isActiveChannel()) {
                    channelWrapper = createChannel(getHost(), getPort());
                }
            }
        }
        return channelWrapper;
    }

    private ChannelWrapper createChannel(String host, int port)
            throws InterruptedException {
        ChannelFuture connectFuture = bootstrap.connect(host, port);
        if (!connectFuture.await(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_CONNECTION_TIMEOUT,
                String.format("client connect to server[%s,%d] timeout", host, port));
        }
        Channel channel = connectFuture.channel();
        if (channel == null || !channel.isActive()) {
            throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_INVOKER_CLOSED,
                String.format("connection to server[%s,%d] has been closed", host, port));
        }
        return new ChannelWrapper(channel);
    }

    @AllArgsConstructor
    private static class ClientResponseFuture implements ResponseFuture<Command> {
        private long id;
        private CompletableFuture<Command> future;
        private NettyResponseHolder responseHolder;

        @Override
        public long getMessageId() {
            return id;
        }

        @Override
        public CompletableFuture<Command> getFuture() {
            return future;
        }

        @Override
        public Optional<CompletableFuture<Command>> destroy() {
            return responseHolder.getAndRemove(id)
                    .map(responseFuture -> responseFuture.getFuture());
        }
    }
}
