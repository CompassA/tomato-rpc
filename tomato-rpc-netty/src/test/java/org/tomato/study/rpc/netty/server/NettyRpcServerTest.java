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
import org.tomato.study.rpc.core.error.TomatoRpcException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class NettyRpcServerTest {

    private NettyRpcServer nettyRpcServer;

    @Before
    public void init() {
        this.nettyRpcServer = spy(new NettyRpcServer("127.0.0.1", 6454));
    }

    /**
     * create 4 threads to call {@link NettyRpcServer#start()}
     * check if only one thread succeed
     */
    @Test
    public void doubleStartTest() throws InterruptedException, TomatoRpcException {
        Runnable runnable = () -> {
            try {
                this.nettyRpcServer.init();
                this.nettyRpcServer.start();
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

        verify(nettyRpcServer, times(1)).doInit();
        verify(nettyRpcServer, times(1)).doStart();
    }


    @Test
    public void doubleStopTest() throws TomatoRpcException {
        nettyRpcServer.init();
        nettyRpcServer.start();
        nettyRpcServer.stop();
        nettyRpcServer.stop();

        verify(nettyRpcServer, times(1)).doStop();
        Assert.assertTrue(nettyRpcServer.isClosed());
    }
}
