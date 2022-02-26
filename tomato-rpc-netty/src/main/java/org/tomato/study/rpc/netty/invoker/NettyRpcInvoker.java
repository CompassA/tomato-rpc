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

package org.tomato.study.rpc.netty.invoker;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.ResponseFuture;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.RpcClient;
import org.tomato.study.rpc.core.base.BaseRpcInvoker;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.data.NettyInvocationResult;
import org.tomato.study.rpc.netty.transport.client.NettyRpcClient;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端调用者，负责与一个RPC服务的某个具体节点通信
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvoker extends BaseRpcInvoker {

    private final RpcClient<Command> rpcClient;

    /**
     * 定时任务，将超时的Future删除
     */
    private final HashedWheelTimer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS);

    public NettyRpcInvoker(MetaData nodeInfo, RpcConfig rpcConfig) {
        super(nodeInfo, rpcConfig);
        URI uri = URI.create("tomato://" + nodeInfo.getHost() + ":" + nodeInfo.getPort());
        this.rpcClient = new NettyRpcClient(uri, rpcConfig.getClientKeepAliveMilliseconds());
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws TomatoRpcException {
        // 将方法调用的数据转化为协议对象
        Command rpcRequest = CommandFactory.request(
                invocation.cloneInvocationWithoutContext(),
                getSerializer(),
                invocation.fetchContextMap(),
                CommandType.RPC_REQUEST);

        // 发送数据
        ResponseFuture<Command> responseFuture = rpcClient.send(rpcRequest);

        // 设置客户端超时
        addTimeoutTask(invocation, responseFuture);
        return new NettyInvocationResult(responseFuture);
    }

    private void addTimeoutTask(Invocation invocation, ResponseFuture<Command> responseFuture) {
        Long timeoutMs = invocation.fetchContextParameter(RpcParameterKey.TIMEOUT)
                .map(Long::valueOf)
                .orElse(getRpcConfig().getGlobalClientTimeoutMilliseconds());
        timer.newTimeout(new RpcTimeoutTask(responseFuture, invocation), timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && rpcClient.isUsable();
    }

    @Override
    protected void doDestroy() throws TomatoRpcException {
        rpcClient.stop();
    }

    @Slf4j
    @RequiredArgsConstructor
    private static class RpcTimeoutTask implements TimerTask {

        private final ResponseFuture<Command> responseFuture;
        private final Invocation invocation;

        @Override
        public void run(Timeout timeout) throws Exception {
            responseFuture.destroy().ifPresent(future -> {
                future.completeExceptionally(
                        new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_CLIENT_TIMEOUT.create()));
                log.warn("rpc timeout, message id: {}, invocation: {}",
                        responseFuture.getMessageId(),
                        invocation);
            });
        }
    }
}
