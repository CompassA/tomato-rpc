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

package org.tomato.study.rpc.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.base.BaseRpcServer;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.observer.LifeCycle;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.handler.DispatcherHandler;
import org.tomato.study.rpc.netty.handler.MetricHandler;
import org.tomato.study.rpc.utils.MetricHolder;

import java.util.concurrent.TimeUnit;

/**
 * netty rpc server, receive client rpc requests
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
public class NettyRpcServer extends BaseRpcServer {

    private static final String BOSS_GROUP_THREAD_NAME = "rpc-server-boss-thread";
    private static final String WORKER_GROUP_THREAD_NAME = "rpc-server-worker-thread";

    /**
     * handler with dispatcher logic
     */
    private DispatcherHandler dispatcherHandler;

    private MetricHolder metricHolder;

    private MetricHandler metricHandler;

    private ServerBootstrap serverBootstrap;

    private Channel channel;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    /**
     * create a rpc server by host and port
     * @param host application host
     * @param port rpc server port to be exported
     */
    public NettyRpcServer(String host, int port) {
        super(host, port);
    }

    @Override
    protected synchronized void doInit() throws TomatoRpcException {
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup(1, new DefaultThreadFactory(BOSS_GROUP_THREAD_NAME));
            this.workerGroup = new EpollEventLoopGroup(new DefaultThreadFactory(WORKER_GROUP_THREAD_NAME));
        } else {
            this.bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(BOSS_GROUP_THREAD_NAME));
            this.workerGroup = new NioEventLoopGroup(new DefaultThreadFactory(WORKER_GROUP_THREAD_NAME));
        }
        this.metricHolder = new MetricHolder();
        this.metricHandler = new MetricHandler(this.metricHolder);
        this.dispatcherHandler = new DispatcherHandler();
        this.serverBootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 10000)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast("frame-decoder",new NettyFrameDecoder())
                                .addLast("proto-decoder", new NettyProtoDecoder())
                                .addLast("frame-encoder", new NettyFrameEncoder())
                                .addLast("metric-handler", NettyRpcServer.this.metricHandler)
                                .addLast("dispatcher-handler", NettyRpcServer.this.dispatcherHandler);
                    }
                });
    }

    @Override
    protected synchronized void doStart() throws TomatoRpcException {
        try {
            channel = serverBootstrap.bind(getPort())
                    .sync()
                    .channel();
            metricHolder.startConsoleReporter(10, TimeUnit.SECONDS);
            metricHolder.startJmxReporter();
        } catch (InterruptedException exception) {
            throw new TomatoRpcException(NettyRpcErrorEnum.CORE_SERVICE_START_ERROR.create(
                    "thread was interrupted when bind"),
                    exception
            );
        }
    }

    @Override
    protected synchronized void doStop() throws TomatoRpcException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channel.close();
        metricHolder.stop();
    }

    @Override
    public boolean isClosed() {
        return getState() == LifeCycle.STOP;
    }
}
