package org.tomato.study.rpc.netty.server;

import org.tomato.study.rpc.netty.codec.netty.NettyFrameDecoder;
import org.tomato.study.rpc.netty.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.netty.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.netty.server.handler.CommandHandler;
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
import org.tomato.study.rpc.core.HandlerRegistry;

import java.io.IOException;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public class NettyRpcServer implements RpcServer {

    @Getter
    private final String host;

    @Getter
    private final int port;

    private final CommandHandler commandHandler;

    private final ServerBootstrap serverBootstrap;

    private volatile Channel channel = null;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    public NettyRpcServer(String host, int port, HandlerRegistry handlerRegistry) {
        this.host = host;
        this.port = port;
        this.commandHandler = new CommandHandler(handlerRegistry);
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup();
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
        }
        this.serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
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
    public synchronized void start() throws InterruptedException {
        if (!this.isClosed()) {
            throw new IllegalStateException();
        }
        this.channel = this.serverBootstrap
                .bind(port)
                .sync()
                .channel();
    }

    @Override
    public synchronized void close() throws IOException {
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
        return this.channel == null;
    }
}
