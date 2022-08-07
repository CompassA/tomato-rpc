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

package org.tomato.study.rpc.dashboard.config;

import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.dashboard.dao.NameServerManager;
import org.tomato.study.rpc.dashboard.dao.ZookeeperRegistryDAO;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({DashboardProperties.class})
public class ZookeeperConfiguration {

    private final DashboardProperties dashboardProperties;

    @Bean
    public CuratorFramework curatorFramework() {
        String nameServerUrl = Optional.ofNullable(System.getProperty(RpcJvmConfigKey.NAME_SERVICE_URI))
                .orElse(dashboardProperties.getNameServerUrl());
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(nameServerUrl)
                .retryPolicy(new ExponentialBackoffRetry(1000, 1000000))
                .connectionTimeoutMs(15 * 1000)
                .sessionTimeoutMs(60 * 1000)
                .namespace("tomato")
                .build();
        client.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> client.close()));
        return client;
    }

    @Bean
    public ZookeeperRegistryDAO zookeeperRegistryDAO(CuratorFramework curatorFramework) {
        return new ZookeeperRegistryDAO(curatorFramework);
    }

    @Bean
    public NameServerManager nameServerManager(ZookeeperRegistryDAO zookeeperRegistryDAO) {
        return new NameServerManager(zookeeperRegistryDAO);
    }
}
