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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.dashboard.dao.NameServerManager;
import org.tomato.study.rpc.dashboard.dao.ZookeeperRegistryDAO;

import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({DashboardProperties.class})
public class TomatoRpcDashboardConfiguration {

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
    public ThreadPoolExecutor refreshRouterExecutor() {
        ThreadPoolExecutor.AbortPolicy abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        return new ThreadPoolExecutor(
            5,
            20,
            10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(5000),
            new DefaultThreadFactory("router-refresh"),
            (r, executor) -> {
                log.error("router-refresh thread pool is overload");
                abortPolicy.rejectedExecution(r, executor);
            }
        );
    }

    @Bean
    public CloseableHttpClient httpClient() {
        // 连接池管理器
        PoolingHttpClientConnectionManager poolingConnManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(1000) // 全局最大连接数
            .setMaxConnPerRoute(1) // 每个host最大连接数
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(20, TimeUnit.SECONDS) // 连接超时
                    .setSocketTimeout(30, TimeUnit.SECONDS)  // Socket超时
                    .setTimeToLive(60, TimeUnit.SECONDS)     // 连接存活时间
                    .build()
            ).build();


        RequestConfig requestConfig = RequestConfig.custom()
            // 连接池等待超时
            .setConnectionRequestTimeout(10, TimeUnit.SECONDS)
            // 响应超时
            .setResponseTimeout(30, TimeUnit.SECONDS)
            .build();

        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(poolingConnManager)
            .disableCookieManagement()                     // 禁用Cookie管理
            .disableAuthCaching()                          // 禁用认证缓存
            .disableConnectionState()                      // 禁用连接状态
            .setUserAgent("TomatoRpcDashboard")              // 设置User-Agent
            .evictExpiredConnections()                     // 自动清理过期连接
            .evictIdleConnections(TimeValue.ofMinutes(5))  // 清理空闲连接
            .build();
    }

    @Bean
    public ZookeeperRegistryDAO zookeeperRegistryDAO(CuratorFramework curatorFramework,
                                                     CloseableHttpClient httpClient,
                                                     ThreadPoolExecutor refreshRouterExecutor) {
        ObjectMapper mapper = new ObjectMapper()
            // 设置时间格式
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
            // java8相关类型支持，如LocalDateTime
            .findAndRegisterModules()
            // 反序列化的时候如果多了其他属性,不抛出异常
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 如果是空对象的时候,不抛异常
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            // 反序列化时忽略null属性
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            // 设置可解析字段
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return new ZookeeperRegistryDAO(curatorFramework, httpClient, refreshRouterExecutor, mapper);
    }

    @Bean
    public NameServerManager nameServerManager(ZookeeperRegistryDAO zookeeperRegistryDAO) {
        return new NameServerManager(zookeeperRegistryDAO);
    }
}
