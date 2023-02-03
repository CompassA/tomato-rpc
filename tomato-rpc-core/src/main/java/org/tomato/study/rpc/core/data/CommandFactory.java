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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.serializer.Serializer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 创建数据帧
 * @author Tomato
 * Created on 2021.04.03
 */
public final class CommandFactory {

    /**
     * request id generator
     */
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    /**
     * create request command
     * @param requestData body
     * @param serializer serialize the body
     * @param type command type
     * @param <T> request type
     * @return request command
     */
    public static <T> Command request(T requestData,
                                      Serializer serializer,
                                      Map<String, String> contextParameters,
                                      CommandType type) {
        Command.CommandBuilder commandBuilder = Command.builder();
        if (requestData != null) {
            byte[] body = serializer.serialize(requestData);
            commandBuilder.header(createHeader(type, serializer, body.length)).body(body);
        } else {
            commandBuilder.header(createHeader(type, serializer, 0));
        }
        Command command = commandBuilder.build();
        if (MapUtils.isNotEmpty(contextParameters)) {
            ExtensionHeaderBuilder extensionHeaderBuilder = new ExtensionHeaderBuilder(command);
            for (ExtensionHeader header : ExtensionHeader.values()) {
                String value = contextParameters.get(header.getName());
                if (StringUtils.isNotBlank(value)) {
                    extensionHeaderBuilder.putParam(header.getName(), value);
                }
            }
            return extensionHeaderBuilder.build();
        }
        return command;
    }

    /**
     * create response command
     * @param requestId request id generated by client
     * @param responseData response data
     * @param serializer serialize the response data
     * @param type response command type
     * @param <T> response data type
     * @return response command
     */
    public static <T> Command response(long requestId,
                                       T responseData,
                                       Serializer serializer,
                                       CommandType type) {
        if (responseData == null) {
            return Command.builder()
                    .header(createHeader(requestId, type, serializer, 0))
                    .build();
        }

        byte[] body = serializer.serialize(responseData);
        return Command.builder()
                .header(createHeader(requestId, type, serializer, body.length))
                .body(body)
                .build();
    }

    /**
     * 为command设置新的body
     * @param command 协议数据
     * @param newBody 新的body
     */
    public static void changeBody(Command command, byte[] newBody) {
        byte[] originBody = command.getBody();
        int originBodyLength = originBody == null ? 0 : originBody.length;

        int newBodyLength = newBody == null ? 0 : newBody.length;
        command.setBody(newBody);

        Header header = command.getHeader();
        header.setLength(header.getLength() - originBodyLength + newBodyLength);
    }

    private static Header createHeader(CommandType type,
                                       Serializer serializer,
                                       int bodyLength) {
        return createHeader(null, type, serializer, bodyLength);
    }

    private static Header createHeader(Long id,
                                       CommandType type,
                                       Serializer serializer,
                                       int bodyLength) {
        return Header.builder()
                .magicNumber(ProtoConstants.MAGIC_NUMBER)
                .version(ProtoConstants.CURRENT_VERSION)
                .id(id == null ? ID_GENERATOR.incrementAndGet() : id)
                .messageType(type.getId())
                .serializeType(serializer.serializerIndex())
                .extensionLength(0)
                .length(bodyLength + ProtoConstants.HEAD_FIX_LENGTH)
                .build();
    }

}
