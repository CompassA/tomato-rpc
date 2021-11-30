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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.tomato.study.rpc.core.api.TomatoApi;

import java.util.Optional;

/**
 * rpc接口元数据
 * @author Tomato
 * Created on 2021.09.29
 */
@Getter
@Builder
@AllArgsConstructor
public class ApiConfig<T> {

    /**
     * 服务唯一标识，必传
     */
    private final String microServiceId;

    /**
     * rpc接口，必传
     */
    private final Class<T> api;

    /**
     * 调用超时等待时间，非必传
     */
    @Setter
    private Long timeoutMs;

    /**
     * 分组，非必传，若未配置，与自身同组
     */
    @Setter
    private String group;

    /**
     * rpc-service节点数据，非必传，客户端直连时传这个参数
     */
    @Setter
    private MetaData nodeInfo;


    public static <T> Optional<ApiConfig<T>> create(Class<T> api) {
        return create(api, null);
    }

    public static <T> Optional<ApiConfig<T>> create(Class<T> api, Long timeoutMs) {
        if (!api.isInterface()) {
            return Optional.empty();
        }
        TomatoApi apiInfo = api.getAnnotation(TomatoApi.class);
        if (apiInfo == null) {
            return Optional.empty();
        }
        return Optional.of(
                ApiConfig.<T>builder()
                        .microServiceId(apiInfo.microServiceId())
                        .api(api)
                        .timeoutMs(timeoutMs)
                        .build()
        );
    }
}
