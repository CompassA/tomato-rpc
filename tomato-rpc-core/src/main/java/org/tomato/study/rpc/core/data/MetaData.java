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
import lombok.ToString;

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
@ToString
public class MetaData {

    /**
     * micro-service-id parameter key name in the uri
     */
    private static final String ID_PARAM_NAME = "micro-service-id";

    /**
     * stage parameter key name in the uri
     */
    private static final String STAGE_PARAM_NAME = "stage";

    /**
     * group parameter key name in the uri
     */
    private static final String GROUP_PARAM_NAME = "group";

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
     * provider micro service id
     */
    private final String microServiceId;

    /**
     * provider stage
     */
    private final String stage;

    /**
     * provider version
     */
    private final String group;

    /**
     * is metadata valid
     * @return true valid
     */
    public boolean isValid() {
        return this.protocol != null && !this.protocol.isBlank()
                && this.host != null && !this.host.isBlank()
                && this.microServiceId != null && !this.microServiceId.isBlank()
                && this.stage != null && !this.stage.isBlank()
                && this.group != null && !this.group.isBlank();
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
                ID_PARAM_NAME, metaData.getMicroServiceId(),
                STAGE_PARAM_NAME, metaData.getStage(),
                GROUP_PARAM_NAME, metaData.getGroup()))
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
        String microServiceId = paramMap.get(ID_PARAM_NAME);
        String stage = paramMap.get(STAGE_PARAM_NAME);
        String group = paramMap.get(GROUP_PARAM_NAME);
        MetaData metaData = MetaData.builder()
                .protocol(scheme)
                .host(host)
                .port(port)
                .microServiceId(microServiceId)
                .stage(stage)
                .group(group)
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
                Objects.equals(this.getMicroServiceId(), otherMataData.getMicroServiceId()) &&
                Objects.equals(this.getStage(), otherMataData.getStage()) &&
                Objects.equals(this.getGroup(), otherMataData.getGroup());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Objects.hashCode(this.getProtocol());
        hash = hash * 31 + Objects.hashCode(this.getHost());
        hash = hash * 31 + Objects.hashCode(this.getPort());
        hash = hash * 31 + Objects.hashCode(this.getMicroServiceId());
        hash = hash * 31 + Objects.hashCode(this.getStage());
        hash = hash * 31 + Objects.hashCode(this.getGroup());
        return hash;
    }
}
