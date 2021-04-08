package org.tomato.study.rpc.impl.sender.netty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.08
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponseHolder {

    private ConcurrentMap<String, NettyResponse> responseMap = new ConcurrentHashMap<>(0);

}
