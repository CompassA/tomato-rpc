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

package org.tomato.study.rpc.netty.transport.client;

import io.netty.util.HashedWheelTimer;
import org.tomato.study.rpc.core.data.Command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * rpc client stores the mapping "client request message id -> client future",
 * when rpc client received the response from the rpc server,
 * it will search the future mapping using the message id in the server response
 * @author Tomato
 * Created on 2021.04.08
 */
public class NettyResponseHolder {

    /**
     * client method call default timeout seconds
     */
    private static final long DEFAULT_TIMEOUT_SECONDS = 20;

    /**
     * message id -> response future
     */
    private final ConcurrentMap<Long, NettyResponseFuture> responseMap = new ConcurrentHashMap<>(0);

    /**
     * schedule tasks to handle rpc timeout
     */
    private final HashedWheelTimer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS);

    public void putFeatureResponse(long id, CompletableFuture<Command> future) {
        putFeatureResponse(id, future, DEFAULT_TIMEOUT_SECONDS);
    }

    public void putFeatureResponse(
            long id,
            CompletableFuture<Command> future,
            long timeoutSeconds) {
        responseMap.computeIfAbsent(
                id, messageId -> createFuture(messageId, future, timeoutSeconds)
        );
    }

    public Optional<NettyResponseFuture> getAndRemove(long id) {
        return Optional.ofNullable(responseMap.remove(id));
    }

    private NettyResponseFuture createFuture(long messageId,
                                             CompletableFuture<Command> future,
                                             long timeoutSeconds) {
        // create future
        NettyResponseFuture responseFuture = new NettyResponseFuture(messageId, future, System.nanoTime());

        // register timeout task
        timer.newTimeout(timeout -> {
            NettyResponseFuture timeoutFuture = responseMap.remove(messageId);
            if (timeoutFuture == null) {
                return;
            }
            timeoutFuture.changeStateToTimeout();
        }, timeoutSeconds, TimeUnit.SECONDS);

        return responseFuture;
    }
}
