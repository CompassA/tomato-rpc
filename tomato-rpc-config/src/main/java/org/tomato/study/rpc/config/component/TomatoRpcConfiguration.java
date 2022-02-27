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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.tomato.study.rpc.config.controller.MonitorController;
import org.tomato.study.rpc.config.data.TomatoRpcProperties;
import org.tomato.study.rpc.config.error.TomatoRpcConfigurationErrorEnum;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcCoreServiceFactory;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.util.Collections;
import java.util.Optional;

/**
 * Spring配置
 * @author Tomato
 * Created on 2021.11.18
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties({TomatoRpcProperties.class})
public class TomatoRpcConfiguration {

    private TomatoRpcProperties properties;

    @Bean
    public RpcCoreService rpcCoreService() throws TomatoRpcException {
        RpcConfig.Builder rpcConfigBuilder = RpcConfig.builder();
        if (StringUtils.isBlank(properties.getMicroServiceId())) {
            throw new TomatoRpcException(TomatoRpcConfigurationErrorEnum.MICROSERVICE_ID_NOT_FOUND.create());
        }
        rpcConfigBuilder.microServiceId(properties.getMicroServiceId());
        rpcConfigBuilder.subscribedServiceIds(CollectionUtils.isNotEmpty(properties.getSubscribedServices())
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
        if (properties.getServerIdleCheckMs() != null) {
            rpcConfigBuilder.serverIdleCheckMilliseconds(properties.getServerIdleCheckMs());
        }
        if (properties.getClientKeepAliveMs() != null) {
            rpcConfigBuilder.clientKeepAliveMilliseconds(properties.getClientKeepAliveMs());
        }
        if (properties.isEnableCircuit()) {
            rpcConfigBuilder.enableCircuit(properties.isEnableCircuit());
            if (properties.getCircuitOpenRate() != null) {
                rpcConfigBuilder.circuitOpenRate(properties.getCircuitOpenRate() / 100.0);
            }
            if (properties.getCircuitOpenSeconds() != null) {
                rpcConfigBuilder.circuitOpenSeconds(properties.getCircuitOpenSeconds());
            }
            if (properties.getCircuitWindow() != null) {
                rpcConfigBuilder.circuitWindow(properties.getCircuitWindow());
            }
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

    @Bean
    public RpcStubPostProcessor rpcStubPostProcessor(RpcCoreService rpcCoreService) {
        return new RpcStubPostProcessor(rpcCoreService);
    }

    @Bean
    public MonitorController monitorController(RpcCoreService rpcCoreService) {
        return new MonitorController(rpcCoreService);
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        // 启动rpc服务
        RpcCoreService rpcCoreService = context.getBean(RpcCoreService.class);
        try {
            rpcCoreService.init();
            rpcCoreService.start();
        } catch (TomatoRpcException e) {
            log.error(e.getErrorInfo().toString());
            throw new TomatoRpcRuntimeException(
                    TomatoRpcConfigurationErrorEnum.RPC_CORE_SERVICE_BEAN_START_ERROR.create());
        }

        // 清除stub缓存
        context.getBean(RpcStubPostProcessor.class).cleanCache();
    }

    @EventListener
    public void onApplicationEvent(ContextClosedEvent event) {
        RpcCoreService rpcCoreService = event.getApplicationContext().getBean(RpcCoreService.class);
        try {
            rpcCoreService.stop();
        } catch (TomatoRpcException e) {
            log.error(e.getErrorInfo().toString());
            throw new TomatoRpcRuntimeException(
                    TomatoRpcConfigurationErrorEnum.RPC_CORE_SERVICE_STOP_ERROR.create());
        }
    }
}
