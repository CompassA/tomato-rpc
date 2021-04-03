package org.tomato.study.rpc.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * rpc protocol frame
 * every rpc request/response will send a command to server/client
 * @author Tomato
 * Created on 2021.03.31
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    /**
     * frame header, fix length
     */
    private Header header;

    /**
     * frame extension header, dynamic length
     */
    private List<Parameter> extension;

    /**
     * frame payload data which has been serialized
     */
    private byte[] body;
}
