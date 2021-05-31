package org.tomato.study.rpc.core.data;

/**
 * command constants
 * @author Tomato
 * Created on 2021.04.03
 */
public interface ProtoConstants {

    /**
     * command magic number num
     */
    byte MAGIC_NUMBER = (byte) 0xac;

    /**
     * command version 1
     */
    int VERSION1 = 20210403;

    /**
     * command current header
     */
    int CURRENT_VERSION = VERSION1;

    /**
     * command header fixed length
     */
    int HEAD_FIX_LENGTH = 19;

}
