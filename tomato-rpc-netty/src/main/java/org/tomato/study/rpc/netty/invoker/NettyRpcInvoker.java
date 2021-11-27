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

import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.RpcClient;
import org.tomato.study.rpc.core.base.BaseRpcInvoker;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.netty.data.NettyInvocationResult;
import org.tomato.study.rpc.netty.transport.client.NettyRpcClient;

import java.net.URI;

/**
 * RPC客户端调用者，负责与一个RPC服务的某个具体节点通信
 * @author Tomato
 * Created on 2021.07.11
 */
public class NettyRpcInvoker extends BaseRpcInvoker {

    private final RpcClient rpcClient;

    public NettyRpcInvoker(MetaData nodeInfo, long keepAliveMs, long timeoutMs) {
        super(nodeInfo);
        this.rpcClient = new NettyRpcClient(
                URI.create("tomato://" + nodeInfo.getHost() + ":" + nodeInfo.getPort()),
                keepAliveMs,
                timeoutMs);
    }

    @Override
    public Result invoke(Invocation invocation) throws TomatoRpcException {
        Command rpcRequest = CommandFactory.request(
                invocation, commandSerializer, CommandType.RPC_REQUEST);
        return new NettyInvocationResult(rpcClient.send(rpcRequest));
    }

    @Override
    public void destroy() throws TomatoRpcException {
        rpcClient.stop();
    }
}
