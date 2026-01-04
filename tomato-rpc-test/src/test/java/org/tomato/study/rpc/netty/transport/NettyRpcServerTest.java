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

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.DefalultProviderRegistry;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.netty.TestCommonUtil;
import org.tomato.study.rpc.netty.transport.client.ChannelWrapper;
import org.tomato.study.rpc.netty.transport.client.NettyRpcClient;
import org.tomato.study.rpc.netty.transport.server.NettyRpcServer;

import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(RpcServerConfig.builder().useBusinessThreadPool(true).businessThreadPoolSize(1).port(39999).build(), new DefalultProviderRegistry()));

        // 最终只会有一个线程完整执行完
        CountDownLatch subWait = new CountDownLatch(1);
        CountDownLatch mainWait = new CountDownLatch(4);
        Runnable runnable = () -> {
            try {
                subWait.await();

                nettyRpcServer.init();
                nettyRpcServer.start();
                nettyRpcServer.stop();
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                mainWait.countDown();
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

        subWait.countDown();
        mainWait.await();

        // 只会在init时调用
        verify(nettyRpcServer, times(1)).isUseBusinessPool();

        // 只会在start时调用
        verify(nettyRpcServer, times(1)).getPort();

        // 只会在stop时调用
        assertTrue(nettyRpcServer.getDispatcherHandler().getBusinessExecutor().isShutdown());
    }

    /**
     * 测试空闲检测
     */
    @Test
    @SuppressWarnings("all")
    public void idleTest() throws TomatoRpcException, InterruptedException {
        // 创建服务端, 并设置客户端发送keep alive时间大于空闲检测时间
        RpcServerConfig rpcConfig = RpcServerConfig.builder()
                .serverReadIdleCheckMilliseconds(1000)
                .clientKeepAliveMilliseconds(1000 * 2)
                .port(40000)
                .build();
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(rpcConfig, new DefalultProviderRegistry()));
        nettyRpcServer.init();
        nettyRpcServer.start();

        // 创建一个客户端
        NettyRpcClient nettyRpcClient = new NettyRpcClient(
                URI.create("tomato://" + rpcConfig.getHost() + ":" + rpcConfig.getPort()),
                rpcConfig.getClientKeepAliveMilliseconds());


        // 连接上服务端并且什么消息都不发, 并在服务端设置的空闲检测的时间内睡眠
        ChannelWrapper channelWrapper;
        try {
            channelWrapper = nettyRpcClient.createOrReconnect();
            Thread.sleep(rpcConfig.getServerReadIdleCheckMilliseconds() * 2);
        } catch (InterruptedException | TimeoutException e) {
            Assert.fail();
            return;
        }

        // 睡醒后发一个消息, 看看是否会抛出ClosedChannelException异常
        boolean hasChannelClosedException = false;
        try {
            Assert.assertFalse(channelWrapper.isActiveChannel());
            channelWrapper.getChannel().writeAndFlush(TestCommonUtil.mockCommand()).sync();
        } catch (Exception e) {
            hasChannelClosedException = e instanceof ClosedChannelException;
        }

        channelWrapper.closeChannel();
        nettyRpcServer.stop();
        nettyRpcClient.stop();
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
        RpcServerConfig rpcConfig = RpcServerConfig.builder()
                .serverReadIdleCheckMilliseconds(1000)
                .clientKeepAliveMilliseconds(500)
                .port(40001)
                .build();
        NettyRpcServer nettyRpcServer = spy(new NettyRpcServer(rpcConfig, new DefalultProviderRegistry()));
        nettyRpcServer.init();
        nettyRpcServer.start();

        // 创建一个客户端
        NettyRpcClient nettyRpcClient = new NettyRpcClient(
                URI.create("tomato://" + rpcConfig.getHost() + ":" + rpcConfig.getPort()),
                rpcConfig.getClientKeepAliveMilliseconds());

        // 连接上服务端并且什么消息都不发, 并在服务端设置的空闲检测的时间内睡眠睡眠一段时间
        ChannelWrapper channelWrapper;
        try {
            channelWrapper = nettyRpcClient.createOrReconnect();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
            return;
        }

        // 睡醒后看看连接是否依然活跃
        assertTrue(channelWrapper.isActiveChannel());
        nettyRpcClient.stop();
    }

}
