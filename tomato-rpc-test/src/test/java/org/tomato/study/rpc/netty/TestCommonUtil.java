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

package org.tomato.study.rpc.netty;

import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.data.ProtoConstants;

/**
 * @author Tomato
 * Created on 2021.04.16
 */
public final class TestCommonUtil {

    /**
     * 1b magic + 4b length + 19b fixed header + 8b extension + 12b body
     * @return mock command
     */
    public static Command mockCommand() {
        byte[] extension = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        byte[] body = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

        return Command.builder()
                .header(Header.builder()
                        .magicNumber(ProtoConstants.MAGIC_NUMBER)
                        .length(body.length + extension.length + ProtoConstants.HEAD_FIX_LENGTH)
                        .version(ProtoConstants.CURRENT_VERSION)
                        .extensionLength(extension.length)
                        .messageType((short) 1)
                        .serializeType((byte) 2)
                        .id(12)
                        .build())
                .extension(extension)
                .body(body)
                .build();
    }
}
