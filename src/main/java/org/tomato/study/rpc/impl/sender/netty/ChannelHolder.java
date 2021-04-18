package org.tomato.study.rpc.impl.sender.netty;

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
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.SpiLoader;
import org.tomato.study.rpc.impl.codec.netty.NettyFrameEncoder;
import org.tomato.study.rpc.impl.codec.netty.NettyProtoDecoder;
import org.tomato.study.rpc.impl.handler.netty.ResponseHandler;

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
     * service provider discovery
     */
    private final NameService nameService = SpiLoader.load(NameService.class);

    /**
     * serviceURI -> service connection info
     */
    private final ConcurrentMap<URI, ChannelWrapper> channelMap;

    /**
     * client event loop
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * connection timeout, time unit: ms
     */
    private final long connectionTimeout;

    public ChannelHolder() {
        this.connectionTimeout = 20;
        this.channelMap = new ConcurrentHashMap<>(0);
        this.eventLoopGroup = Epoll.isAvailable() ?
                new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    /**
     * get the connection with the service provider by serviceVip,
     * if the connection is not established, create a new channel
     * @param serviceVip service provider vip
     * @return netty channel
     * @throws Exception any exception during service discovery and connection register.
     */
    public ChannelWrapper getChannelWrapper(String serviceVip) throws Exception {
        URI serviceURI = nameService.lookupService(serviceVip);
        ChannelWrapper channelWrapper = channelMap.get(serviceURI);
        if (channelWrapper == null) {
            synchronized (ChannelHolder.class) {
                channelWrapper = channelMap.get(serviceURI);
                if (channelWrapper == null) {
                    channelWrapper = registerChannel(serviceURI, connectionTimeout);
                }
            }
        }
        return channelWrapper;
    }

    private ChannelWrapper registerChannel(URI serviceURI, long connectionTimeOut)
            throws InterruptedException, TimeoutException {
        Bootstrap bootstrap = new Bootstrap()
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .group(eventLoopGroup)
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
                new InetSocketAddress(serviceURI.getHost(), serviceURI.getPort()));
        if (!connectFuture.await(connectionTimeOut, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("netty connect timeout");
        }
        Channel channel = connectFuture.channel();
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("netty channel is not active");
        }
        ChannelWrapper channelWrapper = new ChannelWrapper(channel);
        channelMap.put(serviceURI, channelWrapper);
        return channelWrapper;
    }
}
