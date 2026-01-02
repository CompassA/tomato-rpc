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

package org.tomato.study.rpc.core.router;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.tomato.study.rpc.core.base.BaseTest;
import org.tomato.study.rpc.core.circuit.CircuitRpcInvoker;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.Result;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.loadbalance.RoundRobinLoadBalance;
import org.tomato.study.rpc.core.serializer.Serializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseMicroServiceTest extends BaseTest {

    private final String mockServiceId = "mock.subscribe.1";

    private Set<MetaData> originMataDataSet;

    private final BaseMicroServiceSpace baseMicroServiceSpace = spy(new TestMicroServiceSpace(mockServiceId));

    @Before
    public void init() {
        originMataDataSet = mockMetadataSet(mockServiceId);
    }

    @After
    public void destroy() throws TomatoRpcException {
        baseMicroServiceSpace.close();
    }

    @Test
    public void refreshTest() throws Exception {
        when(baseMicroServiceSpace.doCreateInvoker(any())).then((invocationOnMock) -> {
            RpcInvoker mockInvoker = mock(RpcInvoker.class);
            when(mockInvoker.getMetadata()).thenReturn(invocationOnMock.getArgument(0));
            return mockInvoker;
        });

        baseMicroServiceSpace.refresh(originMataDataSet);

        checkInvokerMap(baseMicroServiceSpace, originMataDataSet);
    }

    @Test
    public void deleteInvokerTest() throws Exception {
        when(baseMicroServiceSpace.doCreateInvoker(any())).then((invocationOnMock) -> {
            RpcInvoker mockInvoker = mock(RpcInvoker.class);
            when(mockInvoker.getMetadata()).thenReturn(invocationOnMock.getArgument(0));
            return mockInvoker;
        });

        baseMicroServiceSpace.refresh(originMataDataSet);

        // remove test(delete first)
        Set<MetaData> newMetaSet = mockMetadataSet(mockServiceId);
        Iterator<MetaData> iterator = newMetaSet.iterator();
        MetaData deletedInvoker = iterator.next();
        RpcInvoker mockInvoker = baseMicroServiceSpace.getAllInvokers().stream().filter(v -> Objects.equals(v.getMetadata(), deletedInvoker)).findAny().orElse(null);
        iterator.remove();
        baseMicroServiceSpace.refresh(newMetaSet);

        checkInvokerMap(baseMicroServiceSpace, newMetaSet);
        verify(mockInvoker, times(1)).destroy();
        verify(baseMicroServiceSpace, times(originMataDataSet.size())).doCreateInvoker(any());
        List<RpcInvoker> allInvokers = baseMicroServiceSpace.getAllInvokers();
        for (RpcInvoker invoker : allInvokers) {
            verify(invoker, never()).destroy();
        }

        // delete all
        baseMicroServiceSpace.refresh(Collections.emptySet());
        checkInvokerMap(baseMicroServiceSpace, Collections.emptySet());
        for (RpcInvoker invoker : allInvokers) {
            verify(invoker, times(1)).destroy();
        }
    }

    @Test
    public void addInvokerTest() throws TomatoRpcException {
        when(baseMicroServiceSpace.doCreateInvoker(any())).then((invocationOnMock) -> {
            RpcInvoker mockInvoker = mock(RpcInvoker.class);
            when(mockInvoker.getMetadata()).thenReturn(invocationOnMock.getArgument(0));
            return mockInvoker;
        });

        baseMicroServiceSpace.refresh(originMataDataSet);
        verify(baseMicroServiceSpace, times(originMataDataSet.size())).doCreateInvoker(any());

        Set<MetaData> newMetaSet = mockMetadataSet(mockServiceId);
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
        baseMicroServiceSpace.refresh(newMetaSet);
        checkInvokerMap(baseMicroServiceSpace, newMetaSet);
        verify(baseMicroServiceSpace, times(newMetaSet.size())).doCreateInvoker(any());
    }

    @Test
    public void lockUpTest() throws Exception {
        Invocation invocation = mock(Invocation.class);
        when(invocation.getApiId()).thenReturn("mockApiId$api");
        baseMicroServiceSpace.refresh(originMataDataSet);
        Assert.assertTrue(baseMicroServiceSpace.lookUp(invocation).isPresent());
    }

    @Test
    public void refreshRouterTest() throws TomatoRpcException {
        List<String> routers = Arrays.asList(
                """
                user-id % 10 == 0 -> group == "fast"
                """,
                """
                user-id % 10 == 1 -> group == "slow"
                """
                );
        baseMicroServiceSpace.refreshRouter(routers);

        Assert.assertEquals(baseMicroServiceSpace.getDefaultRouters().size() + routers.size(),
                baseMicroServiceSpace.getRouters().size());
    }

    public static class TestMicroServiceSpace extends BaseMicroServiceSpace {

        public TestMicroServiceSpace(String mockServiceId) {
            super(mockServiceId,
                    RpcConfig.builder().microServiceId("mock-client-id").stage("default").group("default").build(),
                    new RoundRobinLoadBalance());
        }

        @Override
        protected CircuitRpcInvoker doCreateCircuitBreaker(RpcInvoker invoker) {
            return null;
        }

        @Override
        protected RpcInvoker doCreateInvoker(MetaData metadata) {
            return new RpcInvoker() {
                public String getGroup() { return metadata.getGroup(); }
                public MetaData getMetadata() { return metadata; }
                public Map<String, String> getInvokerPropertyMap() { return metadata.toMap(); }
                public Serializer getSerializer() {return null;}
                public Result invoke(Invocation invocation) { return null; }
                public boolean isUsable() {return true;}
                public void destroy() throws TomatoRpcException {}
            };
        }
    }
}
