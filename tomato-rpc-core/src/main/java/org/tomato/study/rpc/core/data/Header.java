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
     * message id, each command sent by a client will have a unique id
     */
    private long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return this.magicNumber == header.magicNumber
                && this.length == header.length
                && this.version == header.version
                && this.extensionLength == header.extensionLength
                && this.messageType == header.messageType
                && this.serializeType == header.serializeType
                && this.id == header.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.magicNumber,
                this.length,
                this.version,
                this.extensionLength,
                this.messageType,
                this.serializeType,
                this.id);
    }

    @Override
    public String toString() {
        return "magicNumber: " + magicNumber + "\n" +
                "length: " + length +"\n" +
                "version: " + version +"\n" +
                "extensionLength: " + extensionLength +"\n" +
                "messageType: " + messageType +"\n" +
                "serializeType: " + serializeType +"\n" +
                "id: " + id;
    }
}
