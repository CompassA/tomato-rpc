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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.data.Command;

import java.util.concurrent.CompletableFuture;

/**
 * 包装Future
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NettyResponseFuture {

    private long messageId;

    private CompletableFuture<Command> future;

    private long timeStamp;

    public boolean complete(Command command) {
        return future.complete(command);
    }

    public boolean completeExceptionally(Throwable exception) {
        return future.completeExceptionally(exception);
    }
}
