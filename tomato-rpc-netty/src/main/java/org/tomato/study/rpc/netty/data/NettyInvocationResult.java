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

package org.tomato.study.rpc.netty.data;

import lombok.AllArgsConstructor;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Tomato
 * Created on 2021.07.17
 */
@AllArgsConstructor
public class NettyInvocationResult implements Result {

    private final CompletableFuture<Command> future;

    @Override
    public Response getResultSync() throws ExecutionException, InterruptedException {
        return getResultAsync().get();
    }

    @Override
    public CompletableFuture<Response> getResultAsync() {
        return future.thenApply(command ->
                (Response) SerializerHolder.getSerializer(command.getHeader().getSerializeType())
                        .deserialize(command.getBody(), RpcResponse.class)
        ).exceptionally(exception -> RpcResponse.fail(exception, exception.getMessage()));
    }

}
