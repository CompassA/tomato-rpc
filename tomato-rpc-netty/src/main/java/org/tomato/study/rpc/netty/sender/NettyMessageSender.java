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

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
public class NettyMessageSender implements MessageSender {

    private final URI uri;

    private final ChannelWrapper channelWrapper;

    private final ChannelResponseHolder responseHolder = ChannelResponseHolder.INSTANCE;

    public NettyMessageSender(String host, int port) throws Exception {
        this.uri = URI.create("tomato://" + host + ":" + port);
        this.channelWrapper = ChannelHolder.INSTANCE.getChannelWrapper(this.uri);
    }

    @Override
    public CompletableFuture<Command> send(Command msg) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        long id = msg.getHeader().getId();
        this.responseHolder.putFeatureResponse(id, future);
        this.channelWrapper.getChannel()
                .writeAndFlush(msg)
                .addListener((ChannelFutureListener) futureChannel -> {
                    if (!futureChannel.isSuccess()) {
                        future.completeExceptionally(futureChannel.cause());
                        this.responseHolder.remove(id);
                    }
                });
        return future;
    }

    @Override
    public String getHost() {
        return this.uri.getHost();
    }

    @Override
    public int getPort() {
        return this.uri.getPort();
    }

    @Override
    public void close() {
        this.channelWrapper.closeChannel();
    }
}
