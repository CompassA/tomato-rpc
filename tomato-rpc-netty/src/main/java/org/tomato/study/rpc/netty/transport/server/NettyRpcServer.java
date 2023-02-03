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

package org.tomato.study.rpc.netty.transport.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.server.BaseRpcServer;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.observer.LifeCycle;
import org.tomato.study.rpc.netty.codec.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.NettyProtoDecoder;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.transport.handler.DispatcherHandler;
import org.tomato.study.rpc.netty.transport.handler.MetricHandler;
import org.tomato.study.rpc.netty.transport.handler.ServerIdleCheckHandler;
import org.tomato.study.rpc.utils.MetricHolder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty RPC服务器, 接收RPC客户端的请求并响应
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
public class NettyRpcServer extends BaseRpcServer {

    private static final String BOSS_GROUP_THREAD_NAME = "rpc-server-boss-thread";
    private static final String WORKER_GROUP_THREAD_NAME = "rpc-server-worker-thread";
    private static final String BUSINESS_GROUP_THREAD_NAME = "rpc-server-business-thread";

    /**
     * RPC服务主题业务逻辑
     */
    private DispatcherHandler dispatcherHandler;

    /**
     * RPC服务监控项管理
     */
    private MetricHolder metricHolder;

    /**
     * 监控一些数据
     */
    private MetricHandler metricHandler;

    /**
     * 服务端启动引导类
     */
    private ServerBootstrap serverBootstrap;

    /**
     * 业务线程池
     */
    private ExecutorService businessThreadPool;

    /**
     * Selector线程池
     */
    private EventLoopGroup bossGroup;

    /**
     * IO线程池
     */
    private EventLoopGroup workerGroup;

    public NettyRpcServer(RpcServerConfig rpcServerConfig) {
        super(rpcServerConfig);
    }

    @Override
    protected synchronized void doInit() throws TomatoRpcException {
        this.metricHolder = new MetricHolder();
        this.metricHandler = new MetricHandler(this.metricHolder);
        this.dispatcherHandler = new DispatcherHandler();
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup(1, new DefaultThreadFactory(BOSS_GROUP_THREAD_NAME));
            this.workerGroup = new EpollEventLoopGroup(new DefaultThreadFactory(WORKER_GROUP_THREAD_NAME));
        } else {
            this.bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(BOSS_GROUP_THREAD_NAME));
            this.workerGroup = new NioEventLoopGroup(new DefaultThreadFactory(WORKER_GROUP_THREAD_NAME));
        }
        if (isUseBusinessPool()) {
            int businessPoolSize = getBusinessPoolSize();
            ThreadPoolExecutor.AbortPolicy abortPolicy = new ThreadPoolExecutor.AbortPolicy();
            this.businessThreadPool = new ThreadPoolExecutor(
                    businessPoolSize,
                    businessPoolSize,
                    0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(5000),
                    new DefaultThreadFactory(BUSINESS_GROUP_THREAD_NAME),
                    (r, executor) -> {
                        log.error("business thread pool is overload");
                        abortPolicy.rejectedExecution(r, executor);
                    }
            );
            dispatcherHandler.setBusinessExecutor(businessThreadPool);
        }
        this.serverBootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 10000)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("idle-checker", new ServerIdleCheckHandler(getReadIdleCheckMilliseconds()));
                        pipeline.addLast("frame-decoder", new NettyFrameDecoder());
                        pipeline.addLast("proto-decoder", new NettyProtoDecoder());
                        pipeline.addLast("frame-encoder", new NettyFrameEncoder());
                        pipeline.addLast("metric-handler", NettyRpcServer.this.metricHandler);
                        pipeline.addLast("dispatcher-handler", NettyRpcServer.this.dispatcherHandler);
                    }
                });
    }

    @Override
    protected synchronized void doStart() throws TomatoRpcException {
        try {
            serverBootstrap.bind(getPort()).sync();
            metricHolder.startConsoleReporter(10, TimeUnit.SECONDS);
            metricHolder.startJmxReporter();
        } catch (InterruptedException exception) {
            throw new TomatoRpcException(NettyRpcErrorEnum.LIFE_CYCLE_START_ERROR.create(
                    "thread was interrupted when bind"),
                    exception
            );
        }
    }

    @Override
    protected synchronized void doStop() throws TomatoRpcException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        metricHolder.stop();
        if (businessThreadPool != null) {
            businessThreadPool.shutdown();
        }
    }

    @Override
    public boolean isClosed() {
        return getState() == LifeCycle.STOP;
    }
}
