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

package org.tomato.study.rpc.netty.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.CommandInterceptor;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.ExtensionHeader;
import org.tomato.study.rpc.core.data.ExtensionHeaderBuilder;
import org.tomato.study.rpc.core.utils.GzipUtils;

import java.util.Map;

/**
 * 若客户端配置了压缩，服务端会对请求体解压，对响应体压缩
 * @author Tomato
 * Created on 2022.02.27
 */
public class CompressInterceptor implements CommandInterceptor {

    @Override
    public Command interceptRequest(
            Command command, Map<String, String> extensionHeaders) throws Exception {
        if (!hasCompressHeader(extensionHeaders)) {
            return command;
        }
        byte[] compressedBody = command.getBody();
        if (compressedBody == null || compressedBody.length < 1) {
            return command;
        }
        byte[] unGzipBody = GzipUtils.unGzip(compressedBody);
        CommandFactory.changeBody(command, unGzipBody);
        return command;
    }

    @Override
    public Command postProcessResponse(Command request,
                                       Command response,
                                       Map<String, String> extensionHeaders) throws Exception {
        if (!hasCompressHeader(extensionHeaders)) {
            return response;
        }
        byte[] body = response.getBody();
        if (body == null || body.length < 1) {
            return response;
        }
        byte[] compressedBody = GzipUtils.gzip(body);
        CommandFactory.changeBody(response, compressedBody);
        return new ExtensionHeaderBuilder(response)
                .putParam(ExtensionHeader.COMPRESS.getKeyName(), Boolean.TRUE.toString())
                .build();
    }

    private boolean hasCompressHeader(Map<String, String> extensionHeaders) {
        String value = extensionHeaders.get(ExtensionHeader.COMPRESS.getKeyName());
        return StringUtils.isNotBlank(value) && Boolean.TRUE.toString().equals(value);
    }

}
