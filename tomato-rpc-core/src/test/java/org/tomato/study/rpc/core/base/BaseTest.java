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

import org.junit.Assert;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.utils.ReflectUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
public class BaseTest {

    protected void checkInvokerMap(MicroServiceSpace serviceProvider, Collection<MetaData> mataDataSet) {
        ConcurrentMap<MetaData, RpcInvoker> invokerRegistry = ReflectUtils.reflectGet(
                serviceProvider, BaseMicroServiceSpace.class, "invokerRegistry");
        ConcurrentMap<String, List<RpcInvoker>> invokerMap = ReflectUtils.reflectGet(
                serviceProvider, BaseMicroServiceSpace.class, "sameGroupInvokerMap");

        Assert.assertEquals(invokerRegistry.values().size(), mataDataSet.size());

        for (MetaData metaData : mataDataSet) {
            Assert.assertNotNull(invokerRegistry.get(metaData));
        }

        Map<String, Set<MetaData>> metadataMap = new HashMap<>(0);
        for (MetaData metaData : mataDataSet) {
            metadataMap.computeIfAbsent(metaData.getGroup(), group -> new HashSet<>(0))
                    .add(metaData);
        }
        for (Map.Entry<String, Set<MetaData>> entry : metadataMap.entrySet()) {
            List<RpcInvoker> invokers = invokerMap.get(entry.getKey());
            Assert.assertNotNull(invokers);
            Assert.assertEquals(invokers.size(), entry.getValue().size());
        }
    }

    protected Set<MetaData> mockMetadataSet(String microServiceId) {
        MetaData.NodeProperty p1 = new MetaData.NodeProperty();
        p1.weight = 1;
        MetaData.NodeProperty p2 = new MetaData.NodeProperty();
        p2.weight = 1;
        MetaData.NodeProperty p3 = new MetaData.NodeProperty();
        p3.weight = 1;

        return new HashSet<>(Set.of(
                MetaData.builder()
                        .protocol("tomato")
                        .host("127.0.0.1")
                        .port(6666)
                        .microServiceId(microServiceId)
                        .group("default")
                        .stage("default")
                        .nodeProperty(p1)
                        .build(),
                MetaData.builder()
                        .protocol("tomato")
                        .host("127.0.0.1")
                        .port(7777)
                        .microServiceId(microServiceId)
                        .group("version1")
                        .stage("default")
                        .nodeProperty(p2)
                        .build(),
                MetaData.builder()
                        .protocol("tomato")
                        .host("127.0.0.1")
                        .port(8888)
                        .microServiceId(microServiceId)
                        .group("default")
                        .stage("default")
                        .nodeProperty(p3)
                        .build()
        ));
    }
}
