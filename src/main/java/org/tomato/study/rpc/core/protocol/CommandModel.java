package org.tomato.study.rpc.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.SerializerHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.04
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandModel<T> {

    private Header header;

    private List<Parameter> extension;

    private T body;

    public static <T> CommandModel<T> toModel(Command command, Class<T> bodyType) {
        Header header = command.getHeader();
        Serializer serializer = SerializerHolder.getSerializer(header.getSerializeType());
        return CommandModel.<T>builder()
                .header(header)
                .extension(command.getExtension() == null || command.getExtension().length < 1
                        ? new ArrayList<>(0)
                        : serializer.deserializeList(command.getExtension(), Parameter.class))
                .body(serializer.deserialize(command.getBody(), bodyType))
                .build();
    }
}
