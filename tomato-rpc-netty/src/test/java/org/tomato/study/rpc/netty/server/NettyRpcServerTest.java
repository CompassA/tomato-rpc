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

package org.tomato.study.rpc.netty.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;


/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class NettyRpcServerTest {

//    private NettyRpcServer nettyRpcServer;
//
//    @Before
//    public void init() {
//        this.nettyRpcServer = new NettyRpcServer("127.0.0.1", 6454);
//    }
//
//    /**
//     * create 4 threads to call {@link NettyRpcServer#start()}
//     * check if only one thread succeed
//     */
//    @Test
//    public void doubleStartTest() throws InterruptedException {
//        int[] cnt = new int[1];
//        CountDownLatch cdl = new CountDownLatch(1);
//        Runnable runnable = () -> {
//            try {
//                cdl.await();
//            } catch (InterruptedException e) {
//                // do nothing
//            }
//            if (this.nettyRpcServer.start()) {
//                cnt[0]++;
//            }
//        };
//        Thread thread1 = new Thread(runnable);
//        Thread thread2 = new Thread(runnable);
//        Thread thread3 = new Thread(runnable);
//        Thread thread4 = new Thread(runnable);
//        thread1.start();
//        thread2.start();
//        thread3.start();
//        thread4.start();
//        Thread.sleep(2000);
//        cdl.countDown();
//        thread1.join();
//        thread2.join();
//        thread3.join();
//        thread4.join();
//
//        if (this.nettyRpcServer.start()) {
//            cnt[0]++;
//        }
//        Assert.assertEquals(1, cnt[0]);
//    }
//
//
//    @Test
//    public void doubleStopTest() {
//        this.nettyRpcServer.start();
//        this.nettyRpcServer.close();
//        this.nettyRpcServer.close();
//        Assert.assertTrue(true);
//    }
//
//    @Test
//    public void stateTest() {
//        Assert.assertEquals(this.nettyRpcServer.getState(), NettyRpcServer.SERVER_INIT);
//        this.nettyRpcServer.start();
//        Assert.assertEquals(this.nettyRpcServer.getState(), NettyRpcServer.SERVER_STARTED);
//        this.nettyRpcServer.close();
//        Assert.assertTrue(this.nettyRpcServer.isClosed());
//    }
}
