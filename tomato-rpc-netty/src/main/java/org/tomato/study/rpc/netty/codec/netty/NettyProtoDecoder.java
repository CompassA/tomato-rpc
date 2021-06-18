package org.tomato.study.rpc.netty.codec.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.tomato.study.rpc.netty.utils.CommandUtil;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyProtoDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf byteBuf,
                          List<Object> list) throws Exception {
        list.add(CommandUtil.decode(byteBuf));
    }
}