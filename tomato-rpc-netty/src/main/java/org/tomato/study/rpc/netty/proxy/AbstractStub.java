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

package org.tomato.study.rpc.netty.proxy;

import lombok.Setter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.netty.data.RpcRequest;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.concurrent.ExecutionException;

/**
 * common method of rpc client stub
 * @author Tomato
 * Created on 2021.03.31
 */
public abstract class AbstractStub {

    protected final Serializer serializer = SerializerHolder.getSerializer((byte) 0);

    @Setter
    protected MessageSender messageSender;

    protected Object invokeRemote(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            throw new RuntimeException("rpc methodContext is null");
        }
        Command request = CommandFactory.INSTANCE.request(
                rpcRequest, serializer, CommandType.RPC_REQUEST);
        try {
            return messageSender.send(request).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
