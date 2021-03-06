/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

/**
 * +-------------+-----------------------+---------+---------------------+---------+-----------+---------+------------+------------+
 * | magic number| length exclude magic  | version | extension parameter | command | serialize | message | extension  |   body     |
 * |             | number and this filed  |         |      length         |   type  |    type   |   id    | 0 - MaxInt | 0 - MaxInt |
 * |   1 byte    |       4 bytes         | 4 bytes |      4 bytes        | 2 bytes |   1 byte  | 8 bytes |  bytes     |   bytes    |
 * +-------------+-----------------------+---------+---------------------+---------+-----------+---------+------------+------------+
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
        return Objects.equals(this.header, command.header)
                && Arrays.equals(this.extension, command.extension)
                && Arrays.equals(this.body, command.body);
    }

    @Override
    public int hashCode() {
        int result = this.header.hashCode();
        result = 31 * result + Arrays.hashCode(this.extension);
        result = 31 * result + Arrays.hashCode(this.body);
        return result;
    }

    @Override
    public String toString() {
        return String.format("------header-------\n%s\n" +
                        "------extension-header------\n%s\n" +
                        "------body------\n%s",
                header,
                extension == null || extension.length < 1 ? "" : new String(extension),
                body == null || body.length < 1 ? "" : new String(body));
    }
}
