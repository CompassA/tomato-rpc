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

package org.tomato.study.rpc.dashboard.web.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.dashboard.service.model.RpcProviderModel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcProvidersVO {

    private String appName;
    private List<RpcProviderVO> providers;

    public static Optional<RpcProvidersVO> toVO(List<RpcProviderModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            return Optional.empty();
        }
        String appName = models.get(0).getAppName();
        List<RpcProviderVO> providers = models.stream()
                .map(model -> new RpcProviderVO(model.getMeta(), model.getRpcConfig()))
                .collect(Collectors.toList());
        return Optional.of(new RpcProvidersVO(appName, providers));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RpcProviderVO {
        private MetaData meta;
        private RpcConfig config;
    }

}
