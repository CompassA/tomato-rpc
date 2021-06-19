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

package org.tomato.study.rpc.netty.sender;

import lombok.NoArgsConstructor;
import org.tomato.study.rpc.core.data.Command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@NoArgsConstructor
public class ChannelResponseHolder {

    public static final ChannelResponseHolder INSTANCE = new ChannelResponseHolder();

    private final ConcurrentMap<Long, NettyResponseFuture> responseMap = new ConcurrentHashMap<>(0);

    public void putFeatureResponse(long id, CompletableFuture<Command> future) {
        responseMap.put(id, new NettyResponseFuture(id, future, System.nanoTime()));
    }

    public Optional<NettyResponseFuture> getResponse(long id) {
        return Optional.ofNullable(responseMap.get(id));
    }

    public Optional<NettyResponseFuture> remove(long id) {
        return Optional.ofNullable(responseMap.remove(id));
    }
}
