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

package org.tomato.study.rpc.registry.zookeeper.impl;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.ServiceProvider;
import org.tomato.study.rpc.core.router.ServiceProviderFactory;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.registry.zookeeper.ChildrenListener;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * implements service registry and service router by zookeeper
 * @author Tomato
 * Created on 2021.07.07
 */
public class ZookeeperRegistry {

    /**
     * provider zNode path: /namespace/vip/stage/{providers}
     */
    private static final String PROVIDER_DICTIONARY = "providers";

    /**
     * curator client
     */
    private final CuratorFramework client;

    /**
     * create service provider by factory interface
     */
    private final ServiceProviderFactory providerFactory = SpiLoader.getLoader(ServiceProviderFactory.class).load();

    /**
     * provider zNode path: /{namespace}/vip/stage/providers
     */
    @Getter
    private final String namespace;

    /**
     * the charset of decode/encode
     */
    @Getter
    private final Charset zNodePathCharset;

    /**
     * RPC client provider map: service vip -> provider
     */
    private final ConcurrentMap<String, ServiceProvider> providerMap = new ConcurrentHashMap<>(0);

    /**
     * provider -> children listener
     */
    private final ConcurrentMap<ServiceProvider, ChildrenListener> childrenListenerMap = new ConcurrentHashMap<>(0);

    /**
     * listener -> watcher
     */
    private final ConcurrentMap<ChildrenListener, PathChildrenWatcher> watcherMap = new ConcurrentHashMap<>(0);

    public ZookeeperRegistry(ZookeeperConfig config) {
        this.namespace = config.getNamespace();
        this.zNodePathCharset = config.getCharset();
        this.client = CuratorFrameworkFactory.builder()
                .connectString(config.getConnString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                //15 seconds
                .connectionTimeoutMs(15 * 1000)
                //session timeout
                .sessionTimeoutMs(60 * 1000)
                //root path
                .namespace(this.namespace)
                .build();
        this.client.start();
    }

    /**
     * export provider self to zookeeper
     * @param metaData provider metadata
     * @throws Exception exception during register
     */
    public void register(MetaData metaData) throws Exception {
        Optional<URI> uriOpt = MetaData.convert(metaData);
        if (uriOpt.isEmpty()) {
            return;
        }
        String zNodePath = convertToZNodePath(
                metaData.getVip(),
                metaData.getStage(),
                PROVIDER_DICTIONARY,
                uriOpt.get().toString());
        this.client.create().withMode(CreateMode.EPHEMERAL).forPath(zNodePath);
    }

    /**
     * delete provider metadata on the zookeeper
     * @param metaData provider metadata
     * @throws Exception exception during unregister
     */
    public void unregister(MetaData metaData) throws Exception {
        Optional<URI> uriOpt = MetaData.convert(metaData);
        if (uriOpt.isEmpty()) {
            return;
        }
        String zNodePath = convertToZNodePath(
                metaData.getVip(),
                metaData.getStage(),
                PROVIDER_DICTIONARY,
                uriOpt.get().toString());
        this.client.delete().forPath(zNodePath);
    }

    /**
     * method for PRC client to fetch RPC service metadata
     * @param vips RPC service vip list
     * @throws Exception exception during subscribe
     */
    public void subscribe(Collection<String> vips) throws Exception {
        if (CollectionUtils.isEmpty(vips)) {
            return;
        }
        for (String vip : vips) {
            ServiceProvider serviceProvider = providerMap.computeIfAbsent(vip, providerFactory::create);
            ChildrenListener listener = childrenListenerMap.computeIfAbsent(
                    serviceProvider, provider -> new PathChildrenListener(this));
            PathChildrenWatcher watcher = watcherMap.computeIfAbsent(
                    listener, listenerKey -> new PathChildrenWatcher(client, listener));
            // get metadata list of RPC provider nodes
            List<String> children = this.client.getChildren()
                    .usingWatcher(watcher)
                    .forPath(vip);
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            final List<MetaData> metaDataList = new ArrayList<>(children.size());
            for (String child : children) {
                String providerPathDecoded = URLDecoder.decode(child, this.zNodePathCharset);
                URI providerMetadataURI = URI.create(providerPathDecoded);
                MetaData.convert(providerMetadataURI)
                        .ifPresent(metaDataList::add);
            }

            // refresh client provider data
            serviceProvider.refresh(new HashSet<>(metaDataList));
        }
    }

    /**
     * method for RPC client to unsubscribe RPC provider
     * @param vips vip list to unsubscribe
     */
    public void unsubscribe(Collection<String> vips) throws IOException {
        if (CollectionUtils.isEmpty(vips)) {
            return;
        }
        for (String vip : vips) {
            ServiceProvider provider = providerMap.remove(vip);
            if (provider == null) {
                continue;
            }
            provider.close();
            ChildrenListener listener = childrenListenerMap.remove(provider);
            if (listener == null) {
                continue;
            }
            PathChildrenWatcher watcher = watcherMap.remove(listener);
            if (watcher != null) {
                watcher.unwatch();
            }
        }
    }

    /**
     * update provider instance data
     * @param vip service provider vip
     * @param children current provider node metadata
     */
    public void notify(String vip, Collection<URI> children) {
        if (StringUtils.isBlank(vip) || CollectionUtils.isEmpty(children)) {
            return;
        }
        providerMap.computeIfAbsent(vip, providerFactory::create).refresh(
                children.stream()
                        .map(MetaData::convert)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet())
        );
    }

    public synchronized void close() throws IOException {
        this.client.close();
        this.unsubscribe(this.providerMap.keySet());
    }

    private String convertToZNodePath(String... parts) {
        if (parts == null || parts.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder(0);
        for (String part : parts) {
            builder.append("/").append(URLEncoder.encode(part, this.zNodePathCharset));
        }
        return builder.toString();
    }
}
