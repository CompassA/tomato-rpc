package org.tomato.study.rpc.core.protocol;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public interface ProtoConstants {

    byte MAGIC_NUMBER = (byte) 0xac;

    int VERSION1 = 20210403;

    int CURRENT_VERSION = VERSION1;

    int HEAD_FIX_LENGTH = 24;

}
