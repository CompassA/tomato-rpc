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

package org.tomato.study.rpc.netty.codec;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.netty.TestCommonUtil;

import java.util.concurrent.CountDownLatch;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyCodecTest {

    /** test decode and encode */
    @Test
    public void test() throws InterruptedException {
        CountDownLatch bindCDL = new CountDownLatch(1);
        CountDownLatch handlerCDL = new CountDownLatch(2);
        int port = 12580;
        NettyCodecTestHandler serverHandler = new NettyCodecTestHandler(handlerCDL, "server");
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NettyFrameDecoder())
                                .addLast(new NettyProtoDecoder())
                                .addLast(serverHandler)

                                .addLast(new NettyFrameEncoder());
                    }
                })
                .bind(port)
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()) {
                        bindCDL.countDown();
                    }
                });

        // wait server bind success
        bindCDL.await();

        NettyCodecTestHandler clientHandler = new NettyCodecTestHandler(handlerCDL, "client");
        new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NettyFrameDecoder())
                                .addLast(new NettyProtoDecoder())
                                .addLast(clientHandler);
                    }
                })
                .connect("127.0.0.1", port)
                .addListener((ChannelFutureListener) listener -> {
                    if (listener.isSuccess()) {
                        Command command = TestCommonUtil.mockCommand();
                        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
                        NettyCommandCodec.encode(command, buffer);
                        listener.channel().writeAndFlush(buffer);
                    }
                });

        handlerCDL.await();

        Command client = clientHandler.msg;
        Command server = serverHandler.msg;
        Header clientHeader = client.getHeader();
        Header serverHeader = server.getHeader();
        Assert.assertEquals(clientHeader.getMagicNumber(), serverHeader.getMagicNumber());
        Assert.assertEquals(clientHeader.getVersion(), serverHeader.getVersion());
        Assert.assertEquals(clientHeader.getExtensionLength(), serverHeader.getExtensionLength());
        Assert.assertEquals(clientHeader.getLength(), serverHeader.getLength());
        Assert.assertEquals(clientHeader.getId(), serverHeader.getId());
        Assert.assertEquals(clientHeader.getSerializeType(), serverHeader.getSerializeType());
        Assert.assertEquals(clientHeader.getMessageType(), serverHeader.getMessageType());
        Assert.assertArrayEquals(client.getBody(), server.getBody());
        Assert.assertArrayEquals(client.getExtension(), server.getExtension());
    }

    private static class NettyCodecTestHandler extends SimpleChannelInboundHandler<Command> {

        public CountDownLatch countDownLatch;

        public String name;

        public Command msg;

        public NettyCodecTestHandler(CountDownLatch countDownLatch, String name) {
            this.countDownLatch = countDownLatch;
            this.name = name;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
            this.msg = msg;
            ctx.channel().writeAndFlush(msg);
            countDownLatch.countDown();
        }
    }
}
