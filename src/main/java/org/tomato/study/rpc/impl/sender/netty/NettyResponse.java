package org.tomato.study.rpc.impl.sender.netty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.protocol.Command;

import java.util.concurrent.CompletableFuture;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NettyResponse {

    private String messageId;

    private CompletableFuture<Command> future;

    private long timeStamp;
}
