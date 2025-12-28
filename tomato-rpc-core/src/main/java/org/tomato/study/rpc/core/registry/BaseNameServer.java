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

package org.tomato.study.rpc.core.registry;

import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RefreshInvokerTask;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;
import org.tomato.study.rpc.utils.Logger;

import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseNameServer extends BaseLifeCycleComponent implements NameServer {

    private final NameServerConfig nameServerConfig;

    /**
     * invoker更新队列
     */
    private final BlockingQueue<RefreshInvokerTask> refreshTaskQueue = new ArrayBlockingQueue<>(1000);

    /**
     * 更新invoker的后台线程
     */
    private Thread refreshInvokerThread;

    public BaseNameServer(NameServerConfig nameServerConfig) {
        this.nameServerConfig = nameServerConfig;
    }

    public String getConnString() {
        return nameServerConfig.getConnString();
    }

    public Charset getCharset() {
        return nameServerConfig.getCharset();
    }

    @Override
    public void submitInvokerRefreshTask(RefreshInvokerTask task) throws InterruptedException {
        refreshTaskQueue.put(task);
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        this.refreshInvokerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RefreshInvokerTask task = refreshTaskQueue.take();
                    task.getMicroServiceSpace().refresh(task.getInvokerInfoSet());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable e) {
                    Logger.DEFAULT.error("refresh error", e);
                }
            }
        }, "invoker-refresh-thread");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        refreshInvokerThread.start();
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        refreshInvokerThread.interrupt();
    }
}
