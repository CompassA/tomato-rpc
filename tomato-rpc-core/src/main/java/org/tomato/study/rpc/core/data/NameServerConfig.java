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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 注册中心配置
 * @author Tomato
 * Created on 2021.09.27
 */
@Getter
@AllArgsConstructor
public class NameServerConfig {

    /**
     * 注册中心的地址
     */
    private final String connString;

    /**
     * 注册中心编解码
     */
    private final Charset charset;

    /**
     * 当前RPC node的环境，name server会订阅同环境的其余provider
     */
    private final String stage;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String connString;
        private Charset charset = StandardCharsets.UTF_8;
        private String stage = "default";

        public Builder connString(String connString) {
            this.connString = connString;
            return this;
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public NameServerConfig build() {
            return new NameServerConfig(connString, charset, stage);
        }
    }
}
