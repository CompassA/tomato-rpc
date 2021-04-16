package org.tomato.study.rpc.impl.codec.netty;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public class NettyFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final Integer LENGTH_FILED_OFFSET = 1;
    private static final Integer LENGTH_FILED_BYTE = 4;

    public NettyFrameDecoder() {
        super(Integer.MAX_VALUE, LENGTH_FILED_OFFSET, LENGTH_FILED_BYTE, 0, 0);
    }
}
