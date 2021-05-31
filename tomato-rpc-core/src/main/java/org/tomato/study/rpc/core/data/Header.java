package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * rpc frame header
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {

    //========================frame boundary======================
    /**
     * a constants
     */
    private byte magicNumber;

    /**
     * the length of the frame exclude `magicNumber` and `length`
     */
    private int length;

    //============================================================
    /**
     * tomato rpc protocol version
     */
    private int version;

    /**
     * the length of parameters
     */
    private int extensionLength;

    /**
     * related the command to the handler of server/client
     */
    private short messageType;

    /**
     * indicate to the server/client the serialization method of the body
     */
    private byte serializeType;

    /**
     * message id, each command sent by one server/client will have an unique id
     */
    private long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return magicNumber == header.magicNumber
                && length == header.length
                && version == header.version
                && extensionLength == header.extensionLength
                && messageType == header.messageType
                && serializeType == header.serializeType
                && id == header.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(magicNumber, length, version, extensionLength,
                messageType, serializeType, id);
    }
}
