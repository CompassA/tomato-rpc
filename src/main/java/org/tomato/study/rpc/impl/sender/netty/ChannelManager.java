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
public class ChannelManager {

    /**
     * service provider discovery
     */
    private final NameService nameService;

    /**
     * serviceURI -> service connection info
     */
    private final ConcurrentMap<URI, ChannelContext> channelContextMap;

    /**
     * client event loop
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * connection timeout, time unit: ms
     */
    private final long connectionTimeout;

    public ChannelManager(NameService nameService, long connectionTimeout) {
        this.nameService = nameService;
        this.connectionTimeout = connectionTimeout;
        this.channelContextMap = new ConcurrentHashMap<>(0);
        this.eventLoopGroup = Epoll.isAvailable() ?
                new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    /**
     * get the connection with the service provider by serviceVip
     * @param serviceVip service provider vip
     * @return connection info
     * @throws Exception any exception during service discovery and connection register.
     */
    public ChannelContext getChannel(String serviceVip) throws Exception {
        URI serviceURI = nameService.lookupService(serviceVip);
        ChannelContext channelContext = channelContextMap.get(serviceURI);
        if (channelContext == null) {
            synchronized (ChannelManager.class) {
                channelContext = channelContextMap.get(serviceURI);
                if (channelContext == null) {
                    channelContext = registerChannel(serviceURI, connectionTimeout);
                }
            }
        }
        return channelContext;
    }

    private synchronized ChannelContext registerChannel(URI serviceURI, long connectionTimeOut)
            throws InterruptedException, TimeoutException {
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(Epoll.isAvailable() ?
                        EpollSocketChannel.class : NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {

                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        //todo
                    }
                }).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        ChannelFuture connectFuture = bootstrap.connect(
                new InetSocketAddress(serviceURI.getHost(), serviceURI.getPort()));
        if (!connectFuture.await(connectionTimeOut, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("netty connect timeout");
        }
        Channel channel = connectFuture.channel();
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("netty channel is not active");
        }
        ChannelContext channelContext = new ChannelContext(channel);
        channelContextMap.put(serviceURI, channelContext);
        return channelContext;
    }
}
