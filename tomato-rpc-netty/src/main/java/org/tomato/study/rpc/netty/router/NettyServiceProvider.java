package org.tomato.study.rpc.netty.router;

import org.tomato.study.rpc.core.base.BaseServiceProvider;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.InvokerConfig;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;

import static org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory.CHANNEL_HOLDER_PARAM_KEY;
import static org.tomato.study.rpc.netty.invoker.NettyRpcInvokerFactory.RESPONSE_HOLDER_PARAM_KEY;

/**
 * @author Tomato
 * Created on 2021.10.02
 */
public class NettyServiceProvider extends BaseServiceProvider {

    private final RpcInvokerFactory nettyRpcInvokerFactory = new NettyRpcInvokerFactory();

    private final NettyChannelHolder channelHolder;

    private final NettyResponseHolder responseHolder;

    public NettyServiceProvider(String vip,
                                NettyChannelHolder channelHolder,
                                NettyResponseHolder responseHolder) {
        super(vip);
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    protected RpcInvoker createInvoker(MetaData metaData) {
        return nettyRpcInvokerFactory.create(
                InvokerConfig.builder()
                        .nodeInfo(metaData)
                        .parameter(CHANNEL_HOLDER_PARAM_KEY, channelHolder)
                        .parameter(RESPONSE_HOLDER_PARAM_KEY, responseHolder)
                        .build()
        ).orElse(null);
    }
}
