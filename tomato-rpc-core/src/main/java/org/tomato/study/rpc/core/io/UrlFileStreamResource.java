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

package org.tomato.study.rpc.core.io;

import lombok.Getter;

import java.net.URL;

/**
 * 文件流资源
 * @author Tomato
 * Created on 2021.11.15
 */
@Getter
public class UrlFileStreamResource extends FileStreamResource {

    private static final String FILE_PROTO = "file";
    private static final String JAR_FILE_PROTO = "jar";

    private final URL url;

    public UrlFileStreamResource(URL url) {
        String protocol = url.getProtocol();
        if (!FILE_PROTO.equals(protocol) && !JAR_FILE_PROTO.equals(protocol)) {
            throw new IllegalArgumentException(
                    "url is not file protocol, url protocol: " + protocol);
        }
        this.url = url;
    }

    @Override
    public URL getURL() {
        return this.url;
    }
}
