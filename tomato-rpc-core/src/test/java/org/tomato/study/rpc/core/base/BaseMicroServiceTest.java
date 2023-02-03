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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.Result;
import org.tomato.study.rpc.core.serializer.Serializer;
import org.tomato.study.rpc.core.circuit.CircuitRpcInvoker;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.loadbalance.RoundRobinLoadBalance;
import org.tomato.study.rpc.core.router.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management"})
public class BaseMicroServiceTest extends BaseTest {

    private final String mockServiceId = "mock.subscribe.1";

    private Set<MetaData> originMataDataSet;

    private final MicroServiceSpace provider = Mockito.spy(new TestServiceProvider(mockServiceId));

    @Before
    public void init() {
        originMataDataSet = mockMetadataSet(mockServiceId);
    }

    @After
    public void destroy() throws TomatoRpcException {
        provider.close();
    }

    /**
     * test common refresh execution chain
     * test if {@see org.tomato.study.rpc.registry.zookeeper.impl.BalanceServiceProvider#invokerMap}
     * and {@see org.tomato.study.rpc.registry.zookeeper.impl.BalanceServiceProvider#invokerRegistry}
     * of ServiceProvider are the same as {@link BaseMicroServiceTest#originMataDataSet}
     */
    @Test
    public void refreshTest() throws Exception {
        PowerMockito.when(provider, "createInvoker", ArgumentMatchers.any())
                .thenReturn(PowerMockito.mock(RpcInvoker.class));

        provider.refresh(originMataDataSet);

        checkInvokerMap(provider, originMataDataSet);
    }

    /**
     * 多次更新Provider的测试
     */
    @Test
    public void upgradeRefreshTest() throws Exception {
        provider.refresh(originMataDataSet);

        // remove test
        Set<MetaData> newMetaSet = mockMetadataSet(mockServiceId);
        Iterator<MetaData> iterator = newMetaSet.iterator();
        iterator.next();
        iterator.remove();
        provider.refresh(newMetaSet);
        checkInvokerMap(provider, newMetaSet);

        // add test
        newMetaSet = mockMetadataSet(mockServiceId);
        newMetaSet.add(
                MetaData.builder()
                        .protocol("tomato")
                        .host("127.0.0.1")
                        .port(9999)
                        .microServiceId(mockServiceId)
                        .stage("default")
                        .group("default")
                        .build()
        );
        provider.refresh(newMetaSet);
        checkInvokerMap(provider, newMetaSet);

        provider.refresh(Collections.emptySet());
        checkInvokerMap(provider, Collections.emptySet());
    }

    @Test
    public void lockUpTest() throws Exception {
        Invocation invocation = Mockito.mock(Invocation.class);
        PowerMockito.when(invocation.getApiId()).thenReturn("mockApiId$api");
        provider.refresh(originMataDataSet);
        Assert.assertTrue(provider.lookUp("default", invocation).isPresent());
    }

    public static class TestServiceProvider extends BaseMicroServiceSpace {

        public TestServiceProvider(String mockServiceId) {
            super(mockServiceId, RpcConfig.builder().build(), new RoundRobinLoadBalance());
        }

        @Override
        protected CircuitRpcInvoker doCreateCircuitBreaker(RpcInvoker invoker) {
            return null;
        }

        @Override
        protected RpcInvoker doCreateInvoker(MetaData metadata) {
            return new RpcInvoker() {
                @Override
                public String getGroup() { return metadata.getGroup(); }
                @Override
                public MetaData getMetadata() { return metadata; }
                @Override
                public Serializer getSerializer() {return null;}
                @Override
                public Result invoke(Invocation invocation) { return null; }
                @Override
                public boolean isUsable() {return true;}
                @Override
                public void destroy() throws TomatoRpcException {}
            };
        }
    }
}
