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
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.base.BaseRpcServer;
import org.tomato.study.rpc.core.observer.LifeCycle;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.handler.CommandHandler;

/**
 * netty rpc server, receive client rpc requests
 * @author Tomato
 * Created on 2021.04.18
 */
@Slf4j
public class NettyRpcServer extends BaseRpcServer {

    /**
     * handler with common logic
     */
    private CommandHandler commandHandler;

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
    protected void doInit() {
        this.commandHandler = new CommandHandler();
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
        }
        this.serverBootstrap = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new NettyFrameDecoder())
                                .addLast(new NettyProtoDecoder())
                                .addLast(NettyRpcServer.this.commandHandler)
                                .addLast(new NettyFrameEncoder());
                    }
                });
    }

    @Override
    protected void doStart() {
        try {
            channel = serverBootstrap.bind(getPort()).sync().channel();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doStop() {
        this.channel.close();
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    @Override
    public boolean isClosed() {
        return getState() == LifeCycle.STOP;
    }
}
