package org.tomato.study.rpc.netty.serializer;

import org.tomato.study.rpc.core.Serializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.04.01
 */
public final class SerializerHolder {

    private static final ConcurrentMap<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static {
        ProtostuffSerializer protostuffSerializer = new ProtostuffSerializer();
        SERIALIZER_MAP.put(protostuffSerializer.serializerIndex(), protostuffSerializer);
    }

    public static Serializer getSerializer(final byte key) {
        return SERIALIZER_MAP.get(key);
    }
}
