package org.tomato.study.rpc.impl.serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.tomato.study.rpc.core.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Protostuff
 * @author Tomato
 * Created on 2021.04.03
 */
public class ProtostuffSerializer implements Serializer {

    private static final ThreadLocal<LinkedBuffer> linkedBufferThreadLocal = new ThreadLocal<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> byte[] serialize(T object) {
        Class<T> clazz = (Class<T>) object.getClass();
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        LinkedBuffer linkedBuffer = getLinkedBuffer();
        try {
            return ProtostuffIOUtil.toByteArray(object, schema, linkedBuffer);
        } finally {
            linkedBuffer.clear();
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }

    @Override
    public byte serializerIndex() {
        return 0;
    }

    @Override
    public <T> byte[] serializeList(List<T> list, Class<T> memberType) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Schema<T> schema = RuntimeSchema.getSchema(memberType);
        LinkedBuffer linkedBuffer = getLinkedBuffer();
        try {
            ProtostuffIOUtil.writeListTo(byteStream, list, schema, linkedBuffer);
            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        } finally {
            linkedBuffer.clear();
        }
    }

    @Override
    public <T> List<T> deserializeList(byte[] listData, Class<T> memberType) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(listData);
        Schema<T> schema = RuntimeSchema.getSchema(memberType);
        LinkedBuffer linkedBuffer = getLinkedBuffer();
        try {
            return ProtostuffIOUtil.parseListFrom(byteStream, schema);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(0);
        } finally {
            linkedBuffer.clear();
        }
    }

    private LinkedBuffer getLinkedBuffer() {
        return Optional.ofNullable(linkedBufferThreadLocal.get())
                .orElseGet(() -> {
                    LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
                    linkedBufferThreadLocal.set(buffer);
                    return buffer;
                });
    }
}
