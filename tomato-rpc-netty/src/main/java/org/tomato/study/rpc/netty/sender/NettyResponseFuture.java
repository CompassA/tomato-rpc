package org.tomato.study.rpc.netty.sender;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.data.Command;

import java.util.concurrent.CompletableFuture;

/**
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
}
