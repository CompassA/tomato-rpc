package org.tomato.study.rpc.impl.serialize;

import org.tomato.study.rpc.core.Serializer;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public class ProtostuffSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] data) {
        return null;
    }

    @Override
    public byte serializerIndex() {
        return 0;
    }
}
