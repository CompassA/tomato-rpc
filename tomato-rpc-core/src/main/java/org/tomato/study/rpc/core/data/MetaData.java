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
import lombok.Setter;
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

    public static final String URL_FORMAT = "%s://%s:%d/?%s=%s&%s=%s&%s=%s&%s=%s";

    /**
     * micro-service-id parameter key name in the uri
     */
    public static final String ID_PARAM_NAME = "micro-service-id";

    /**
     * stage parameter key name in the uri
     */
    public static final String STAGE_PARAM_NAME = "stage";

    /**
     * group parameter key name in the uri
     */
    public static final String GROUP_PARAM_NAME = "group";

    /**
     * other dynamic property
     */
    public static final String PROPERTY_KEY = "property";

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
     * provider property
     */
    @Setter
    private NodeProperty nodeProperty;

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
        String url = String.format(URL_FORMAT,
                metaData.getProtocol(),
                metaData.getHost(),
                metaData.getPort(),
                ID_PARAM_NAME, metaData.getMicroServiceId(),
                STAGE_PARAM_NAME, metaData.getStage(),
                GROUP_PARAM_NAME, metaData.getGroup(),
                PROPERTY_KEY, metaData.getNodeProperty().toUrl());
        return Optional.of(URI.create(url));
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
        NodeProperty property = toNodeProperty(paramMap.get(PROPERTY_KEY));
        MetaData metaData = MetaData.builder()
                .protocol(scheme)
                .host(host)
                .port(port)
                .microServiceId(microServiceId)
                .stage(stage)
                .group(group)
                .nodeProperty(property)
                .build();
        if (!metaData.isValid()) {
            return Optional.empty();
        }
        return Optional.of(metaData);
    }

    public Map<String, String> toMap() {
        Map<String, String> res = new HashMap<>();
        res.put(GROUP_PARAM_NAME, group);
        res.put(NodeProperty.WEIGHT_KEY, String.valueOf(nodeProperty.weight));
        return res;
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

    public int getWeight() {
        return nodeProperty.weight;
    }

    public static class NodeProperty {
        public static final String DELIMITER = ":";
        public static final String PROPERTY_DELIMITER = "#";
        public static final String WEIGHT_KEY = "weight";

        /**
         * 均衡负载权重
         */
        public int weight;

        public String toUrl() {
            return String.format("%s%s%s", WEIGHT_KEY, DELIMITER, weight);
        }

        @Override
        public String toString() {
            return toUrl();
        }
    }

    /**
     * 解析属性键值对
     * @param propertyStr "weight:123#key2:abc"
     * @return 实体对象
     */
    private static NodeProperty toNodeProperty(String propertyStr) {
        String[] split = propertyStr.split(NodeProperty.PROPERTY_DELIMITER);
        NodeProperty nodeProperty = new NodeProperty();
        for (String part : split) {
            String[] kv = part.split(NodeProperty.DELIMITER);
            if (NodeProperty.WEIGHT_KEY.equals(kv[0])) {
                nodeProperty.weight = Integer.parseInt(kv[1]);
            } else {

            }
        }
        return nodeProperty;
    }
}
