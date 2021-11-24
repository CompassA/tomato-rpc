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
import org.tomato.study.rpc.core.api.TomatoApi;

import java.util.Optional;

/**
 * todo 实现其余配置
 * rpc接口元数据
 * @author Tomato
 * Created on 2021.09.29
 */
@Getter
@Builder
@AllArgsConstructor
public class ApiConfig<T> {

    /**
     * 服务唯一标识
     */
    private final String microServiceId;

    /**
     * rpc接口
     */
    private final Class<T> api;

    public static <T> Optional<ApiConfig<T>> create(Class<T> api) {
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
                        .build()
        );
    }
}
