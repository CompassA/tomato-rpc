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

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.core.router.ServiceProvider;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * load balance service provider
 * @author Tomato
 * Created on 2021.07.10
 */
@Builder
@AllArgsConstructor
public class BalanceServiceProvider implements ServiceProvider {

    /**
     * service provider vip {@link MetaData#getVip()}
     */
    private final String vip;

    /**
     * SPI invoker factory
     */
    private final RpcInvokerFactory invokerFactory = SpiLoader.getLoader(RpcInvokerFactory.class).load();

    /**
     * registered invoker
     */
    private final ConcurrentMap<MetaData, RpcInvoker> invokerRegistry = new ConcurrentHashMap<>(0);

    /**
     * version {@link MetaData#getVersion()} -> RpcInvokers with same version
     */
    private final ConcurrentMap<String, List<RpcInvoker>> invokerMap = new ConcurrentHashMap<>(0);

    @Override
    public String getVIP() {
        return this.vip;
    }

    @Override
    public Optional<RpcInvoker> lookUp(String version) {
        List<RpcInvoker> invokers = this.invokerMap.get(version);
        if (CollectionUtils.isEmpty(invokers)) {
            return Optional.empty();
        }
        return Optional.of(invokers.get((int) (Math.random() * invokers.size())));
    }

    @Override
    public void refresh(Set<MetaData> metadataSet) throws IOException {
        if (CollectionUtils.isEmpty(metadataSet)) {
            this.close();
            return;
        }
        // version -> metadata set
        Map<String, Set<MetaData>> metadataMap = new HashMap<>(0);
        for (MetaData metaData : metadataSet) {
            metadataMap.computeIfAbsent(metaData.getVersion(), version -> new HashSet<>(0))
                    .add(metaData);
        }

        // do refresh
        for (Map.Entry<String, Set<MetaData>> entry : metadataMap.entrySet()) {
            String version = entry.getKey();
            Set<MetaData> newMetadataSet = entry.getValue();
            List<RpcInvoker> newInvokers = new ArrayList<>(newMetadataSet.size());

            // add new invokers, if the invoker is present, don't register
            for (MetaData metadata : newMetadataSet) {
                RpcInvoker rpcInvoker = this.invokerRegistry.computeIfAbsent(
                        metadata,
                        key -> this.invokerFactory.create(key).orElse(null)
                );
                if (rpcInvoker != null) {
                    newInvokers.add(rpcInvoker);
                }
            }

            // close invokers
            List<RpcInvoker> oldInvokers = CollectionUtils.isEmpty(newInvokers)
                    ? this.invokerMap.remove(version)
                    : this.invokerMap.put(version, newInvokers);
            if (CollectionUtils.isEmpty(oldInvokers)) {
                continue;
            }
            for (RpcInvoker oldInvoker : oldInvokers) {
                // only close the invoker which is not in the metadataSet
                if (newInvokers.contains(oldInvoker)) {
                    continue;
                }
                this.invokerRegistry.remove(oldInvoker.getMetadata());
                try {
                    oldInvoker.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (CollectionUtils.isNotEmpty(this.invokerRegistry.values())) {
            for (RpcInvoker value : this.invokerRegistry.values()) {
                value.close();
            }
        }
        this.invokerRegistry.clear();
        this.invokerMap.clear();
    }
}
