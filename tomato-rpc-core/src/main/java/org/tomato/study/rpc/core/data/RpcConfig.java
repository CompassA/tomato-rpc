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
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * necessary config data for rpc
 * @author Tomato
 * Created on 2021.07.11
 */
@Getter
@AllArgsConstructor
public class RpcConfig {

    /**
     * network proto
     */
    private final String protocol;

    /**
     * a rpc application has a unique vip
     */
    private final String serviceVIP;

    /**
     * rpc application stage
     */
    private final String stage;

    /**
     * rpc application version
     */
    private final String version;

    /**
     * other vips subscribed by current rpc application
     */
    private final List<String> subscribedVIP;

    /**
     * name server connect uri
     */
    private final String nameServiceURI;

    /**
     * rpc local server port
     */
    private final int port;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String protocol = "tomato";
        private String serviceVIP;
        private String stage = "default";
        private String version = "default";
        private List<String> subscribedVIP = Collections.emptyList();
        private String nameServiceURI;
        private int port = 9090;

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder serviceVIP(String serviceVIP) {
            this.serviceVIP = serviceVIP;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder subscribedVIP(List<String> subscribedVIP) {
            this.subscribedVIP = subscribedVIP;
            return this;
        }

        public Builder nameServiceURI(String nameServiceURI) {
            this.nameServiceURI = nameServiceURI;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public RpcConfig build() {
            return new RpcConfig(
                    this.protocol,
                    this.serviceVIP,
                    this.stage,
                    this.version,
                    this.subscribedVIP,
                    this.nameServiceURI,
                    this.port
            );
        }
    }
}
