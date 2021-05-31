package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

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
    private byte[] extension;

    /**
     * frame payload data which has been serialized
     */
    private byte[] body;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(header, command.header)
                && Arrays.equals(extension, command.extension)
                && Arrays.equals(body, command.body);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(header);
        result = 31 * result + Arrays.hashCode(extension);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }
}
