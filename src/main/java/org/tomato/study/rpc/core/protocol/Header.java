package org.tomato.study.rpc.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * rpc frame header
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Header {

    /**
     * a constants
     */
    private byte magicNumber;

    /**
     * the length of header and parameters
     */
    private int headerLength;

    /**
     * the length of command payload body
     */
    private int bodyLength;

    /**
     * tomato rpc protocol version
     */
    private short version;

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
}
