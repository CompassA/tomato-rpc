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

import lombok.RequiredArgsConstructor;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.ResponseFuture;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.netty.serializer.SerializerHolder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Tomato
 * Created on 2021.07.17
 */
@RequiredArgsConstructor
public class NettyInvocationResult implements Result {

    private final ResponseFuture<Command> future;

    @Override
    public Response getResultSync() throws ExecutionException, InterruptedException {
        try {
            // 抄写dubbo，貌似这样性能能好一点
            return getResultAsync().get(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return new Response() {
                @Override
                public int getCode() {
                    return Code.FAIL.getCode();
                }

                @Override
                public Object getData() {
                    return new Object();
                }

                @Override
                public String getMessage() {
                    return "wait sync timeout";
                }
            };
        }
    }

    @Override
    public CompletableFuture<Response> getResultAsync() {
        return future.getFuture().thenApply(response ->
                (Response) SerializerHolder.getSerializer(response.getHeader().getSerializeType())
                        .deserialize(response.getBody(), RpcResponse.class)
        ).exceptionally(exception ->
                RpcResponse.fail(exception.getCause(), exception.getMessage())
        );
    }

}
