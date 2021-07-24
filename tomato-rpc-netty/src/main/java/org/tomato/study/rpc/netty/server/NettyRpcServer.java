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
import lombok.Getter;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.handler.CommandHandler;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * netty rpc server, receive client rpc requests
 * @author Tomato
 * Created on 2021.04.18
 */
public class NettyRpcServer implements RpcServer {

    /**
     * cas updater for {@link NettyRpcServer#state}
     * state list:
     * {@link NettyRpcServer#SERVER_INIT}
     * {@link NettyRpcServer#SERVER_STARTED}
     * {@link NettyRpcServer#SERVER_STOPPED}
     */
    private static final AtomicIntegerFieldUpdater<NettyRpcServer> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(NettyRpcServer.class, "state");

    public static final int SERVER_INIT = 0;
    public static final int SERVER_STARTED = 1;
    public static final int SERVER_STOPPED = 2;

    /**
     * server state
     */
    @Getter
    @SuppressWarnings({ "unused", "FieldMayBeFinal" })
    private volatile int state;

    /**
     * application host
     */
    @Getter
    private final String host;

    /**
     * exported port
     */
    @Getter
    private final int port;

    /**
     * handler with common logic
     */
    private final CommandHandler commandHandler;

    private final ServerBootstrap serverBootstrap;

    private Channel channel;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    /**
     * create a rpc server by host and port
     * @param host application host
     * @param port rpc server port to be exported
     */
    public NettyRpcServer(String host, int port) {
        this.state = SERVER_INIT;
        this.host = host;
        this.port = port;
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
    public boolean start() {
        if (!STATE_UPDATER.compareAndSet(this, SERVER_INIT, SERVER_STARTED)) {
            return false;
        }
        try {
            this.channel = this.serverBootstrap.bind(this.port)
                    .sync()
                    .channel();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() {
        if (!STATE_UPDATER.compareAndSet(this, SERVER_STARTED, SERVER_STOPPED)) {
            return;
        }
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
            this.bossGroup = null;
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
    }

    @Override
    public boolean isClosed() {
        return STATE_UPDATER.get(this) == SERVER_STOPPED;
    }
}
