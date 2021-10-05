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

package org.tomato.study.rpc.core.base;

import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.ServiceProvider;

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
 * 一个RPC服务，管理服务的多个实例
 * @author Tomato
 * Created on 2021.07.10
 */
public abstract class BaseServiceProvider implements ServiceProvider {

    /**
     * 服务唯一标识 {@link MetaData#getVip()}
     */
    private final String vip;

    /**
     * 一个RPC服务的所有实例节点，一个RpcInvoker持有一个与服务实例节点的连接并与其通信
     * 实例节点ip端口等元数据信息 -> 对应的Invoker
     */
    private final ConcurrentMap<MetaData, RpcInvoker> invokerRegistry = new ConcurrentHashMap<>(0);

    /**
     * 按RPC服务的实例版本对Invoker进行分类
     * {@link MetaData#getVersion()} -> RpcInvokers with same version
     */
    private final ConcurrentMap<String, List<RpcInvoker>> sameVersionInvokerMap = new ConcurrentHashMap<>(0);

    public BaseServiceProvider(String vip) {
        this.vip = vip;
    }

    @Override
    public String getVIP() {
        return vip;
    }

    /**
     * 根据客户端订阅的该RPC服务的版本，寻找匹配的服务实例节点
     * @param version service version RPC服务
     * @return 可与实例节点通信的RpcInvoker
     */
    @Override
    public Optional<RpcInvoker> lookUp(String version) {
        List<RpcInvoker> invokers = sameVersionInvokerMap.get(version);
        if (CollectionUtils.isEmpty(invokers)) {
            return Optional.empty();
        }
        return Optional.of(invokers.get((int) (Math.random() * invokers.size())));
    }

    /**
     * 更新RPC服务的实例节点信息
     * @param newRpcInstanceInfoSet 相同VIP、相同Stage的所有实例节点
     * @throws IOException 更新异常
     */
    @Override
    public void refresh(Set<MetaData> newRpcInstanceInfoSet) throws IOException {
        // 若实例节点为空，表明这个服务已经没有节点了，清空Invoker
        if (CollectionUtils.isEmpty(newRpcInstanceInfoSet)) {
            cleanInvoker();
            return;
        }

        // 将实例节点数据按Version分组
        Map<String, Set<MetaData>> sameVersionMetaMap = new HashMap<>(0);
        for (MetaData metaData : newRpcInstanceInfoSet) {
            sameVersionMetaMap.computeIfAbsent(
                    metaData.getVersion(), version -> new HashSet<>(0)
            ).add(metaData);
        }

        // 对同Version的实例节点数据，进行Invoker创建
        for (Map.Entry<String, Set<MetaData>> entry : sameVersionMetaMap.entrySet()) {
            // 将创建的Invoker收集到一个新的数组
            String version = entry.getKey();
            Set<MetaData> newMetadataSet = entry.getValue();
            List<RpcInvoker> newInvokers = new ArrayList<>(newMetadataSet.size());

            // 若节点对应的Invoker已经创建过，仍使用旧的Invoker，否则重建一个Invoker
            for (MetaData metadata : newMetadataSet) {
                RpcInvoker rpcInvoker = invokerRegistry.computeIfAbsent(
                        metadata,
                        // 不改成方法引用形式, 便于debug
                        metadataKey -> createInvoker(metadataKey)
                );
                if (rpcInvoker != null) {
                    newInvokers.add(rpcInvoker);
                }
            }

            // 获取该RPC服务版本的所有旧Invoker
            List<RpcInvoker> oldInvokers = CollectionUtils.isEmpty(newInvokers)
                    // 若该版本没有新的节点，直接从versionMap中将对应的Invoker删除
                    ? sameVersionInvokerMap.remove(version)
                    // 若该版本有新节点，将旧Invoker从Map中拿出，将新Invoker放入Map
                    : sameVersionInvokerMap.put(version, newInvokers);

            // 如果没有旧的Invoker，说明没有旧的Invoker需要关闭
            if (CollectionUtils.isEmpty(oldInvokers)) {
                continue;
            }

            // 遍历所有的旧Invoker，执行Invoker的关闭操作
            for (RpcInvoker oldInvoker : oldInvokers) {
                // 若这个实例已经在新的Invoker列表中存在了，不进行操作
                if (newInvokers.contains(oldInvoker)) {
                    continue;
                }

                // 从Invoker列表中移除旧的Invoker
                invokerRegistry.remove(oldInvoker.getMetadata());

                // 关闭invoker
                try {
                    oldInvoker.close();
                } catch (IOException e) {
                    // 什么都不做
                }
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        cleanInvoker();
    }

    protected abstract RpcInvoker createInvoker(MetaData metaData);

    private void cleanInvoker() throws IOException {
        if (CollectionUtils.isNotEmpty(invokerRegistry.values())) {
            for (RpcInvoker value : invokerRegistry.values()) {
                value.close();
            }
        }
        invokerRegistry.clear();
        sameVersionInvokerMap.clear();
    }
}
