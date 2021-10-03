package org.tomato.study.rpc.netty.router;

import org.tomato.study.rpc.core.router.ServiceProvider;
import org.tomato.study.rpc.core.router.ServiceProviderFactory;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;

/**
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyServiceProviderFactory implements ServiceProviderFactory {

    private final NettyChannelHolder channelHolder;

    private final NettyResponseHolder responseHolder;

    public NettyServiceProviderFactory(NettyChannelHolder channelHolder,
                                       NettyResponseHolder responseHolder) {
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    public ServiceProvider create(String vip) {
        return new NettyServiceProvider(vip, channelHolder, responseHolder);
    }
}
