/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.data;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.RpcJvmConfigKey;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 创建拓展头部
 * @author Tomato
 * Created on 2022.02.20
 */
public class ExtensionHeaderBuilder {

    private final Command command;
    private final StringBuilder builder;

    public ExtensionHeaderBuilder(Command command) {
        this.command = command;
        this.builder = new StringBuilder();
        byte[] extension = command.getExtension();
        if (extension != null) {
            this.builder.append(new String(extension, StandardCharsets.UTF_8));
        }
    }

    public ExtensionHeaderBuilder putParam(String key, String value) {
        if (key == null || StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return this;
        }
        if (builder.length() > 0) {
            builder.append(RpcJvmConfigKey.ENTRY_DELIMITER);
        }
        builder.append(key)
                .append(RpcJvmConfigKey.KEY_VALUE_DELIMITER)
                .append(value);
        return this;
    }

    public Command build() {
        if (builder.length() < 1) {
            return command;
        }
        byte[] newExtHeaders = builder.toString().getBytes(StandardCharsets.UTF_8);
        command.setExtension(newExtHeaders);

        Header header = command.getHeader();
        header.setLength(header.getLength() - header.getExtensionLength() + newExtHeaders.length);
        header.setExtensionLength(newExtHeaders.length);
        return command;
    }

    /**
     * 获取拓展头部参数
     * @param command 帧
     * @return Map形式的头部参数
     */
    public static Map<String, String> getExtensionHeader(@NonNull Command command) {
        byte[] extension = command.getExtension();
        if (extension == null || extension.length < 1) {
            return Collections.emptyMap();
        }
        return RpcJvmConfigKey.parseMultiKeyValue(new String(extension, StandardCharsets.UTF_8));
    }
}
