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

package org.tomato.study.rpc.netty.transport;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.netty.TestCommonUtil;
import org.tomato.study.rpc.netty.transport.client.ChannelWrapper;
import org.tomato.study.rpc.netty.transport.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.transport.client.NettyResponseHolder;
import org.tomato.study.rpc.netty.transport.handler.KeepAliveHandler;
import org.tomato.study.rpc.netty.transport.handler.ResponseHandler;
import org.tomato.study.rpc.netty.transport.server.NettyRpcServer;

import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class NettyRpcServerTest {

    /**
     * create 4 threads to call {@link NettyRpcServer#start()}
     * check if only one thread succeed
     */
    @Test
    public void doubleStartTest() throws Exception {
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(RpcServerConfig.builder().port(39999).build()));
        Runnable runnable = () -> {
            try {
                nettyRpcServer.init();
                nettyRpcServer.start();
                nettyRpcServer.stop();
            } catch (TomatoRpcException e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        Thread thread3 = new Thread(runnable);
        Thread thread4 = new Thread(runnable);
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        verifyPrivate(nettyRpcServer, times(1)).invoke("doInit");
        verifyPrivate(nettyRpcServer, times(1)).invoke("doStart");
        verifyPrivate(nettyRpcServer, times(1)).invoke("doStop");
    }

    /**
     * 测试空闲检测
     */
    @Test
    @SuppressWarnings("all")
    public void idleTest() throws TomatoRpcException, InterruptedException {
        // 创建服务端, 并设置客户端发送keep alive时间大于空闲检测时间
        RpcServerConfig serverConfig = RpcServerConfig.builder()
                .serverReadIdleCheckMilliseconds(1000)
                .clientKeepAliveMilliseconds(1000 * 2)
                .port(40000)
                .build();
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(serverConfig));
        nettyRpcServer.init();
        nettyRpcServer.start();

        // 创建一个客户端
        NettyChannelHolder nettyChannelHolder = new NettyChannelHolder(
                serverConfig.getClientKeepAliveMilliseconds(),
                Lists.newArrayList(
                        new KeepAliveHandler(),
                        new ResponseHandler(new NettyResponseHolder()))
        );


        // 连接上服务端并且什么消息都不发, 并在服务端设置的空闲检测的时间内睡眠
        ChannelWrapper channelWrapper;
        try {
            channelWrapper = nettyChannelHolder.getOrCreateChannelWrapper(
                    URI.create("tomato://" + serverConfig.getHost() + ":" + serverConfig.getPort()));
            Thread.sleep(serverConfig.getServerReadIdleCheckMilliseconds() * 2);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
            Assert.fail();
            return;
        }

        // 睡醒后发一个消息, 看看是否会抛出ClosedChannelException异常
        boolean hasChannelClosedException = false;
        try {
            Assert.assertFalse(channelWrapper.isActiveChannel());
            channelWrapper.getChannel().writeAndFlush(TestCommonUtil.mockCommand()).sync();
        } catch (Exception e) {
            e.printStackTrace();
            hasChannelClosedException = e instanceof ClosedChannelException;
        }

        channelWrapper.closeChannel();
        nettyRpcServer.stop();
        if (!hasChannelClosedException) {
            Assert.fail();
        }
    }


    /**
     * 测试keep alive
     */
    @Test
    @SuppressWarnings("all")
    public void keepAliveTest() throws TomatoRpcException, InterruptedException {
        // 创建服务端
        RpcServerConfig serverConfig = RpcServerConfig.builder()
                .serverReadIdleCheckMilliseconds(1000)
                .clientKeepAliveMilliseconds(500)
                .port(40001)
                .build();
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(serverConfig));
        nettyRpcServer.init();
        nettyRpcServer.start();

        // 创建一个客户端
        NettyChannelHolder nettyChannelHolder = new NettyChannelHolder(
                serverConfig.getClientKeepAliveMilliseconds(),
                Lists.newArrayList(
                        // 加上keep alive
                        new KeepAliveHandler(),
                        new ResponseHandler(new NettyResponseHolder()))
        );

        // 连接上服务端并且什么消息都不发, 并在服务端设置的空闲检测的时间内睡眠睡眠一段时间
        ChannelWrapper channelWrapper;
        try {
            channelWrapper = nettyChannelHolder.getOrCreateChannelWrapper(
                    URI.create("tomato://" + serverConfig.getHost() + ":" + serverConfig.getPort()));
            Thread.sleep(serverConfig.getServerReadIdleCheckMilliseconds() * 2);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
            return;
        }

        // 睡醒后看看连接是否依然活跃
        Assert.assertTrue(channelWrapper.isActiveChannel());
    }

}
