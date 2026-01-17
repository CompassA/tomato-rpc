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

package org.tomato.study.rpc.core.loadbalance;

import lombok.NoArgsConstructor;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomato
 * Created on 2022.08.02
 */
@NoArgsConstructor
public class RoundRobinLoadBalance extends BaseLoadBalance {

    /**
     * 权重数据
     * api id -> 节点 -> 节点权重
     */
    private final ConcurrentMap<String, ConcurrentMap<MetaData, NodeWeight>> weightMap = new ConcurrentHashMap<>(0);

    @Override
    protected RpcInvoker doSelect(Invocation invocation, List<RpcInvoker> invokers)
            throws TomatoRpcRuntimeException {
        ConcurrentMap<MetaData, NodeWeight> serviceWeightMap =
                weightMap.computeIfAbsent(invocation.getApiId(), key -> new ConcurrentHashMap<>());
        RpcInvoker targetInvoker = null;
        NodeWeight selectedWeightContext = null;
        long currentMaxWeight = Long.MIN_VALUE;
        long sum = 0;

        // 累加标准权重 并选出当前权重最高的节点
        for (RpcInvoker invoker : invokers) {
            MetaData metadata = invoker.getMetadata();
            if (metadata == null || !metadata.isValid()) {
                Logger.DEFAULT.warn("Node MetaData[{}] is invalid, remove from load balance context", metadata);
                continue;
            }
            NodeWeight nodeWeight = serviceWeightMap.computeIfAbsent(
                    metadata, key -> new NodeWeight(metadata.getWeight()));
            if (metadata.getWeight() != nodeWeight.weight) {
                nodeWeight.weight = metadata.getWeight();
            }
            sum += nodeWeight.weight;
            long currentWeight = nodeWeight.addWeightAndGet();
            if (currentWeight > currentMaxWeight) {
                currentMaxWeight = currentWeight;
                targetInvoker = invoker;
                selectedWeightContext = nodeWeight;
            }
        }

        // 目标节点的当前权重减去累加权重
        if (targetInvoker != null) {
            selectedWeightContext.subWeightSum(sum);
        }
        return targetInvoker;
    }

    private static class NodeWeight {
        /**
         * 当前权重
         */
        final AtomicLong currentWeight;
        /**
         * 标准权重
         */
        int weight;

        /**
         * 最后一次更新当前权重的时间
         */
        long lastUpdate;

        public NodeWeight(int weight) {
            this.weight = weight;
            this.currentWeight = new AtomicLong(0);
            this.lastUpdate = System.currentTimeMillis();
        }

        /**
         * 增加权重
         * @return 增加后的权重
         */
        public long addWeightAndGet() {
            lastUpdate = System.currentTimeMillis();
            return currentWeight.addAndGet(weight);
        }

        public void subWeightSum(long weightSum) {
            lastUpdate = System.currentTimeMillis();
            currentWeight.addAndGet(weightSum * -1);
        }
    }
}
