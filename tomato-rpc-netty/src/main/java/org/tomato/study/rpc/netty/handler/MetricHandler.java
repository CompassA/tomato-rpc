package org.tomato.study.rpc.netty.handler;

import com.codahale.metrics.Gauge;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.tomato.study.rpc.utils.MetricHolder;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomato
 * Created on 2021.10.03
 */
@ChannelHandler.Sharable
public class MetricHandler extends ChannelDuplexHandler {

    /**
     * 记录线程数
     */
    private final AtomicLong nettyServerConnectionCounter;

    public MetricHandler(MetricHolder metricHolder) {
        this.nettyServerConnectionCounter = new AtomicLong(0);
        metricHolder.registerMetric("nettyServerConnectionCounter", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return nettyServerConnectionCounter.longValue();
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        nettyServerConnectionCounter.incrementAndGet();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        nettyServerConnectionCounter.decrementAndGet();
        super.channelInactive(ctx);
    }
}
