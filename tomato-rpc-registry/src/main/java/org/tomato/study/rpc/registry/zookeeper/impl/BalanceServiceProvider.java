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
     * invoker creator
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
        List<RpcInvoker> invokers = invokerMap.get(version);
        if (CollectionUtils.isEmpty(invokers)) {
            return Optional.empty();
        }
        return Optional.of(invokers.get(0));
    }

    @Override
    public void refresh(Set<MetaData> metadataSet) {
        if (CollectionUtils.isEmpty(metadataSet)) {
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
                RpcInvoker rpcInvoker = invokerRegistry.computeIfAbsent(
                        metadata, key -> invokerFactory.create(key).orElse(null));
                newInvokers.add(rpcInvoker);
            }

            // close invokers
            List<RpcInvoker> oldInvokers = invokerMap.put(version, newInvokers);
            if (CollectionUtils.isNotEmpty(oldInvokers)) {
                for (RpcInvoker oldInvoker : oldInvokers) {
                    invokerRegistry.remove(oldInvoker.getMetadata());
                    try {
                        oldInvoker.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (CollectionUtils.isNotEmpty(invokerRegistry.values())) {
            for (RpcInvoker value : invokerRegistry.values()) {
                value.close();
            }
        }
        invokerRegistry.clear();
        invokerMap.clear();
    }
}
