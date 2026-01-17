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

package org.tomato.study.rpc.core.invoker;

import lombok.Getter;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.Result;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.serializer.Serializer;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomato
 * Created on 2021.11.26
 */
public abstract class BaseRpcInvoker implements RpcInvoker {

    /**
     * RPC服务节点的ip、端口等数据
     */
    private final MetaData nodeInfo;

    /**
     * nodeInfo的Map形式
     */
    private final Map<String, String> nodeInfoMap;

    /**
     * rpc配置
     */
    @Getter
    private final RpcConfig rpcConfig;

    /**
     * 客户端为请求body体设置的序列化方式
     */
    private final Serializer commandSerializer;

    /**
     * 当前正在调用invoker的线程数
     */
    private final AtomicLong processingCounter;

    /**
     * invoker是否已经关闭
     */
    private volatile boolean closed = false;

    public BaseRpcInvoker(MetaData nodeInfo, RpcConfig rpcConfig) {
        this.nodeInfo = nodeInfo;
        this.nodeInfoMap = nodeInfo.toMap();
        this.rpcConfig = rpcConfig;
        this.commandSerializer = SpiLoader.getLoader(Serializer.class).load();
        this.processingCounter = new AtomicLong(0);
    }

    @Override
    public Result invoke(Invocation invocation) throws TomatoRpcException {
        if (!isUsable()) {
            throw new TomatoRpcException(TomatoRpcErrorEnum.RPC_INVOKER_CLOSED,
                String.format("invoker[%s] is not usable", nodeInfo));
        }
        processingCounter.incrementAndGet();
        try {
            return doInvoke(invocation);
        } finally {
            processingCounter.decrementAndGet();
        }

    }

    @Override
    public boolean isUsable() {
        return !closed;
    }

    @Override
    public void destroy() throws TomatoRpcException {
        if (closed) {
            return;
        }
        synchronized (this) {
            if (closed) {
                return;
            }
            // step1, 设置关闭标志, 此时invoker不会再接收任务
            // step2, 优雅关闭，等60s，若60s没关闭，强行结束
            closed = true;
            for (int i = 0; i < 60; ++i) {
                if (processingCounter.get() == 0) {
                    break;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            doDestroy();
        }
    }

    @Override
    public String getGroup() {
        return nodeInfo.getGroup();
    }

    @Override
    public MetaData getMetadata() {
        return nodeInfo;
    }

    @Override
    public Map<String, String> getInvokerPropertyMap() {
        return nodeInfoMap;
    }

    @Override
    public Serializer getSerializer() {
        return commandSerializer;
    }

    protected abstract Result doInvoke(Invocation invocation) throws TomatoRpcException;

    protected abstract void doDestroy() throws TomatoRpcException;
}
