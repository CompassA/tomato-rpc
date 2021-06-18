package org.tomato.study.rpc.core.data;

import org.tomato.study.rpc.core.Serializer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public enum CommandFactory {

    INSTANCE,
    ;

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    /**
     * create request command
     * @param requestData body
     * @param serializer serialize the body
     * @param type command type
     * @param <T> request type
     * @return request command
     */
    public <T> Command request(T requestData,
                               Serializer serializer,
                               CommandType type) {
        byte[] body = serializer.serialize(requestData);
        return Command.builder()
                .header(createHeader(null, type, serializer, 0, body.length))
                .extension(new byte[0])
                .body(body)
                .build();
    }

    /**
     * create request command with parameters
     * @param requestData request data
     * @param parameters request command parameters
     * @param serializer serialize the request data
     * @param type request command type
     * @param <T> request data type
     * @return request data
     */
    public <T> Command request(T requestData,
                               List<Parameter> parameters,
                               Serializer serializer,
                               CommandType type) {
        byte[] extension = serializer.serializeList(parameters, Parameter.class);
        byte[] body = serializer.serialize(requestData);
        return Command.builder()
                .header(createHeader(null, type, serializer, extension.length, body.length))
                .extension(extension)
                .body(body)
                .build();
    }

    /**
     * create response command
     * @param requestId request id generated by client
     * @param responseData response data
     * @param serializer serialize the response data
     * @param type response command type
     * @param <T> response data type
     * @return response command
     */
    public <T> Command response(long requestId,
                                T responseData,
                                Serializer serializer,
                                CommandType type) {
        byte[] body = serializer.serialize(responseData);
        return Command.builder()
                .header(createHeader(requestId, type, serializer, 0, body.length))
                .extension(new byte[0])
                .body(body)
                .build();
    }

    private Header createHeader(Long id,
                                CommandType type,
                                Serializer serializer,
                                int extensionLength,
                                int bodyLength) {
        return Header.builder()
                .magicNumber(ProtoConstants.MAGIC_NUMBER)
                .version(ProtoConstants.CURRENT_VERSION)
                .id(id == null ? ID_GENERATOR.incrementAndGet() : id)
                .messageType(type.getId())
                .serializeType(serializer.serializerIndex())
                .extensionLength(extensionLength)
                .length(bodyLength + extensionLength + ProtoConstants.HEAD_FIX_LENGTH)
                .build();
    }

}