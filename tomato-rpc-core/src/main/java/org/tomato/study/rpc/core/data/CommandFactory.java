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

    public <T> Command createRequest(T requestData,
                                     Serializer serializer,
                                     CommandType type) {
        byte[] body = serializer.serialize(requestData);
        return Command.builder()
                .header(createHeader(null, type, serializer, 0, body.length))
                .extension(new byte[0])
                .body(body)
                .build();
    }

    public <T> Command createRequest(T requestData,
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

    public <T> Command createResponse(long requestId,
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
