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

import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.data.Command;

import java.util.concurrent.CompletableFuture;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class NettyMessageSender implements MessageSender {

    private final String serviceVIP;

    private final ChannelHolder channelHolder;

    private final ChannelResponseHolder responseHolder;

    public NettyMessageSender(String serviceVIP,
                              ChannelHolder channelHolder,
                              ChannelResponseHolder responseHolder) {
        this.serviceVIP = serviceVIP;
        this.channelHolder = channelHolder;
        this.responseHolder = responseHolder;
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        long id = msg.getHeader().getId();
        responseHolder.putFeatureResponse(id, future);
        try {
            channelHolder.getChannelWrapper(serviceVIP)
                    .getChannel()
                    .writeAndFlush(msg)
                    .addListener((ChannelFutureListener) futureChannel -> {
                        if (!futureChannel.isSuccess()) {
                            future.completeExceptionally(futureChannel.cause());
                            responseHolder.remove(id);
                        }
                    });
        } catch (Throwable e) {
            responseHolder.remove(id);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void close() {
        channelHolder.close();
    }
}
