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

package org.tomato.study.rpc.config.component;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tomato.study.rpc.config.data.TomatoRpcProperties;
import org.tomato.study.rpc.config.error.TomatoRpcConfigurationErrorEnum;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcCoreServiceFactory;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.util.Collections;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.11.18
 */
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({TomatoRpcProperties.class})
public class TomatoRpcConfiguration {

    private TomatoRpcProperties properties;

    @Bean
    public RpcCoreService rpcCoreService() {
        RpcConfig.Builder rpcConfigBuilder = RpcConfig.builder();
        if (StringUtils.isBlank(properties.getMicroServiceId())) {
            throw new TomatoRpcRuntimeException(
                    TomatoRpcConfigurationErrorEnum.MICROSERVICE_ID_NOT_FOUND.create());
        }
        rpcConfigBuilder.serviceVIP(properties.getMicroServiceId());

        rpcConfigBuilder.subscribedVIP(CollectionUtils.isNotEmpty(properties.getSubscribedServices())
                ? properties.getSubscribedServices() : Collections.emptyList());

        if (StringUtils.isNotBlank(properties.getStage())) {
            rpcConfigBuilder.stage(properties.getStage());
        }

        if (properties.getPort() != null) {
            rpcConfigBuilder.port(properties.getPort());
        }

        if (properties.getBusinessThread() != null) {
            rpcConfigBuilder.businessThreadPoolSize(properties.getBusinessThread());
        }

        rpcConfigBuilder.group(
                Optional.ofNullable(System.getProperty(RpcJvmConfigKey.MICRO_SERVICE_GROUP))
                        .orElse(properties.getGroup()));

        rpcConfigBuilder.nameServiceURI(
                Optional.ofNullable(System.getProperty(RpcJvmConfigKey.NAME_SERVICE_URI))
                        .orElse(properties.getNameServiceUri()));


        return SpiLoader.getLoader(RpcCoreServiceFactory.class)
                .load()
                .create(rpcConfigBuilder.build());
    }
}
