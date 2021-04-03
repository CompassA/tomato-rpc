package org.tomato.study.rpc.core;

/**
 * serialization core method
 * @author Tomato
 * Created on 2021.03.31
 */
public interface Serializer {

    /**
     * serialize
     * @param object to be serialized
     * @param <T> origin type
     * @return serialized data
     */
    <T> byte[] serialize(T object);

    /**
     * deserialize
     * @param data serialized data
     * @param <T> target type
     * @return deserialized object
     */
    <T> T deserialize(byte[] data);

    /**
     * describe the serializer index of serializer in command
     * @return serializer class
     */
    byte serializerIndex();
}
