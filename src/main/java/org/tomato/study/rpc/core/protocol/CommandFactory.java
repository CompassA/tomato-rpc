package org.tomato.study.rpc.core.protocol;

import org.tomato.study.rpc.core.Serializer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.tomato.study.rpc.core.protocol.ProtoConstants.*;

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
                .header(createHeader(type, serializer, 0, body.length))
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
                .header(
                        createHeader(
                                type,
                                serializer,
                                extension.length,
                                body.length))
                .extension(extension)
                .body(body)
                .build();
    }

    private Header createHeader(CommandType type,
                                Serializer serializer,
                                int extensionLength,
                                int bodyLength) {
        return Header.builder()
                .magicNumber(MAGIC_NUMBER)
                .version(CURRENT_VERSION)
                .id(ID_GENERATOR.incrementAndGet())
                .messageType(type.getId())
                .serializeType(serializer.serializerIndex())
                .extensionLength(extensionLength)
                .length(bodyLength + extensionLength + HEAD_FIX_LENGTH)
                .build();
    }

}
