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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端调用者，负责与一个RPC服务的某个具体节点通信
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvoker extends BaseRpcInvoker {

    private final RpcClient<Command> rpcClient;
    private final HashedWheelTimer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS);

    public NettyRpcInvoker(MetaData nodeInfo, RpcConfig rpcConfig) {
        super(nodeInfo, rpcConfig);
        URI uri = URI.create("tomato://" + nodeInfo.getHost() + ":" + nodeInfo.getPort());
        this.rpcClient = new NettyRpcClient(uri, rpcConfig.getClientKeepAliveMilliseconds());
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws TomatoRpcException {
        Command rpcRequest = CommandFactory.request(invocation, getSerializer(), CommandType.RPC_REQUEST);
        ResponseFuture<Command> responseFuture = rpcClient.send(rpcRequest);
        CompletableFuture<Command> future = responseFuture.getFuture();
        NettyInvocationResult result = new NettyInvocationResult(future);
        timer.newTimeout(
                timeout -> {
                    future.completeExceptionally(
                            new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_CLIENT_TIMEOUT.create()));
                    responseFuture.destroy();
                },
                // todo 重写超时
                0,
                TimeUnit.MILLISECONDS);
        return result;
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && rpcClient.isUsable();
    }

    @Override
    protected void doDestroy() throws TomatoRpcException {
        rpcClient.stop();
    }
}
