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
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.ResponseFuture;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.ExtensionHeader;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.Result;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcErrorInfo;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.BaseRpcInvoker;
import org.tomato.study.rpc.core.transport.RpcClient;
import org.tomato.study.rpc.core.utils.GzipUtils;
import org.tomato.study.rpc.netty.data.NettyInvocationResult;
import org.tomato.study.rpc.netty.transport.client.NettyRpcClient;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        this.rpcClient = new NettyRpcClient(uri, rpcConfig.clientKeepAliveMilliseconds());
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws TomatoRpcException {
        // 将方法调用的数据转化为协议对象
        Map<String, String> contextMap = InvocationContext.get();
        Command rpcRequest = CommandFactory.request(
                invocation,
                getSerializer(),
                contextMap,
                CommandType.RPC_REQUEST);

        // 进行一些前置处理
        rpcRequest = beforeSendRequest(rpcRequest);

        // 发送数据
        ResponseFuture<Command> responseFuture = rpcClient.send(rpcRequest);

        // 设置客户端超时
        addTimeoutTask(invocation, rpcRequest, responseFuture);
        return new NettyInvocationResult(responseFuture);
    }

    protected Command beforeSendRequest(Command request) {
        if (Objects.equals(Boolean.TRUE.toString(), ExtensionHeader.COMPRESS.getValueFromContext())) {
            CommandFactory.changeBody(request, GzipUtils.gzip(request.getBody()));
        }
        return request;
    }

    private void addTimeoutTask(Invocation invocation,
                                Command request,
                                ResponseFuture<Command> responseFuture) {
        Long timeoutMs = Optional.ofNullable(ExtensionHeader.TIMEOUT.getValueFromContext())
                .map(Long::valueOf)
                .orElse(getRpcConfig().globalClientTimeoutMilliseconds());
        timer.newTimeout(new RpcTimeoutTask(request, responseFuture, invocation),
                timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && rpcClient.isUsable();
    }

    @Override
    protected void doDestroy() throws TomatoRpcException {
        rpcClient.stop();
        timer.stop();
    }

    @RequiredArgsConstructor
    private static class RpcTimeoutTask implements TimerTask {

        private final Command request;
        private final ResponseFuture<Command> responseFuture;
        private final Invocation invocation;

        @Override
        public void run(Timeout timeout) throws Exception {
            responseFuture.destroy().ifPresent(future -> {
                TomatoRpcErrorInfo errorInfo = TomatoRpcCoreErrorEnum.RPC_CLIENT_TIMEOUT
                        .create(String.format("rpc timeout, invocation: %s, request: %s", invocation, request));
                future.completeExceptionally(new TomatoRpcRuntimeException(errorInfo));
                Logger.DEFAULT.warn("rpc timeout, message id: {}, invocation: {}",
                        responseFuture.getMessageId(),
                        invocation);
            });
        }
    }
}
