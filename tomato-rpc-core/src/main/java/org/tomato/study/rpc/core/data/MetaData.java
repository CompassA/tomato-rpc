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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Provider Instance Metadata
 * @author Tomato
 * Created on 2021.06.20
 */
@Getter
@Builder
public class MetaData {

    /**
     * vip parameter key name in the uri
     */
    private static final String VIP_PARAM_NAME = "VIP";

    /**
     * stage parameter key name in the uri
     */
    private static final String STAGE_PARAM_NAME = "stage";

    /**
     * version parameter key name in the uri
     */
    private static final String VERSION_PARAM_NAME = "version";

    /**
     * rpc protocol
     */
    private final String protocol;

    /**
     * provider host
     */
    private final String host;

    /**
     * provider port
     */
    private final int port;

    /**
     * provider vip
     */
    private final String vip;

    /**
     * provider stage
     */
    private final String stage;

    /**
     * provider version
     */
    private final String version;

    /**
     * is metadata valid
     * @return true valid
     */
    public boolean isValid() {
        return this.protocol != null && !this.protocol.isBlank()
                && this.host != null && !this.host.isBlank()
                && this.vip != null && !this.vip.isBlank()
                && this.stage != null && !this.stage.isBlank()
                && this.version != null && !this.version.isBlank();
    }

    /**
     * convert metadata to uri
     * @param metaData origin metaData
     * @return uri
     */
    public static Optional<URI> convert(@NonNull MetaData metaData) {
        if (!metaData.isValid()) {
            return Optional.empty();
        }
        return Optional.of(URI.create(String.format("%s://%s:%d/?%s=%s&%s=%s&%s=%s",
                metaData.getProtocol(),
                metaData.getHost(),
                metaData.getPort(),
                VIP_PARAM_NAME, metaData.getVip(),
                STAGE_PARAM_NAME, metaData.getStage(),
                VERSION_PARAM_NAME, metaData.getVersion()))
        );
    }

    public static Optional<MetaData> convert(URI uri) {
        String query = uri.getQuery();
        String[] parts = query.split("&");
        if (parts.length == 0) {
            return Optional.empty();
        }
        Map<String, String> paramMap = new HashMap<>(0);
        for (String part : parts) {
            String[] split = part.split("=");
            if (split.length != 2) {
                return Optional.empty();
            }
            paramMap.put(split[0], split[1]);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String vip = paramMap.get(VIP_PARAM_NAME);
        String stage = paramMap.get(STAGE_PARAM_NAME);
        String version = paramMap.get(VERSION_PARAM_NAME);
        MetaData metaData = MetaData.builder()
                .protocol(scheme)
                .host(host)
                .port(port)
                .vip(vip)
                .stage(stage)
                .version(version)
                .build();
        if (!metaData.isValid()) {
            return Optional.empty();
        }
        return Optional.of(metaData);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetaData)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        MetaData otherMataData = (MetaData) other;
        return Objects.equals(this.getProtocol(), otherMataData.getProtocol()) &&
                Objects.equals(this.getHost(), otherMataData.getHost()) &&
                Objects.equals(this.getPort(), otherMataData.getPort()) &&
                Objects.equals(this.getVip(), otherMataData.getVip()) &&
                Objects.equals(this.getStage(), otherMataData.getStage()) &&
                Objects.equals(this.getVersion(), otherMataData.getVersion());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Objects.hashCode(this.getProtocol());
        hash = hash * 31 + Objects.hashCode(this.getHost());
        hash = hash * 31 + Objects.hashCode(this.getPort());
        hash = hash * 31 + Objects.hashCode(this.getVip());
        hash = hash * 31 + Objects.hashCode(this.getStage());
        hash = hash * 31 + Objects.hashCode(this.getVersion());
        return hash;
    }
}
