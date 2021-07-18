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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.core.router.RpcInvokerFactory;
import org.tomato.study.rpc.core.router.ServiceProvider;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
@RunWith(PowerMockRunner.class)
public class BalanceServiceProviderTest extends BaseTest {

    private final String mockVIP = "mock.subscribe.1";

    private Set<MetaData> originMataDataSet;

    private RpcInvokerFactory mockInvokerFactory;

    @Mock
    private SpiLoader<RpcInvokerFactory> mockInvokerFactoryLoader;

    @Before
    public void init() {
        this.originMataDataSet = mockMetadataSet(mockVIP);
        this.mockInvokerFactory = metadata -> Optional.of(new RpcInvoker() {
            @Override
            public void close() throws IOException { }
            @Override
            public String getVersion() { return metadata.getVersion(); }
            @Override
            public MetaData getMetadata() { return metadata; }
            @Override
            public Result<Response> invoke(Invocation invocation) { return null; }
        });
    }

    /**
     * test common refresh execution chain
     * test if {@see org.tomato.study.rpc.registry.zookeeper.impl.BalanceServiceProvider#invokerMap}
     * and {@see org.tomato.study.rpc.registry.zookeeper.impl.BalanceServiceProvider#invokerRegistry}
     * of ServiceProvider are the same as {@link BalanceServiceProviderTest#originMataDataSet}
     */
    @Test
    @PrepareForTest({SpiLoader.class})
    public void refreshTest() throws IOException {
        // mock invoker factory
        mockStatic(SpiLoader.class);
        when(SpiLoader.getLoader(eq(RpcInvokerFactory.class))).thenReturn(this.mockInvokerFactoryLoader);
        when(this.mockInvokerFactoryLoader.load()).thenReturn(this.mockInvokerFactory);
        ServiceProvider provider = new BalanceServiceProviderFactory().create("test");

        provider.refresh(this.originMataDataSet);

        checkInvokerMap(provider, this.originMataDataSet);
    }

    @Test
    @PrepareForTest({SpiLoader.class})
    public void upgradeRefreshTest() throws IOException {
        // mock invoker factory
        mockStatic(SpiLoader.class);
        when(SpiLoader.getLoader(eq(RpcInvokerFactory.class))).thenReturn(this.mockInvokerFactoryLoader);
        when(this.mockInvokerFactoryLoader.load()).thenReturn(this.mockInvokerFactory);
        ServiceProvider provider = new BalanceServiceProviderFactory().create("test");
        provider.refresh(this.originMataDataSet);

        // remove test
        Set<MetaData> newMetaSet = mockMetadataSet(mockVIP);
        Iterator<MetaData> iterator = newMetaSet.iterator();
        iterator.next();
        iterator.remove();
        provider.refresh(newMetaSet);
        checkInvokerMap(provider, newMetaSet);

        // add test
        newMetaSet = mockMetadataSet(mockVIP);
        newMetaSet.add(
                MetaData.builder()
                        .protocol("tomato")
                        .host("127.0.0.1")
                        .port(9999)
                        .vip(mockVIP)
                        .stage("default")
                        .version("default")
                        .build()
        );
        provider.refresh(newMetaSet);
        checkInvokerMap(provider, newMetaSet);

        provider.refresh(Collections.emptySet());
        checkInvokerMap(provider, Collections.emptySet());
    }

    @Test
    @PrepareForTest({SpiLoader.class})
    public void lockUpTest() throws IOException {
        // mock invoker factory
        mockStatic(SpiLoader.class);
        when(SpiLoader.getLoader(eq(RpcInvokerFactory.class))).thenReturn(this.mockInvokerFactoryLoader);
        when(this.mockInvokerFactoryLoader.load()).thenReturn(this.mockInvokerFactory);
        ServiceProvider provider = new BalanceServiceProviderFactory().create("test");
        provider.refresh(this.originMataDataSet);

        Assert.assertTrue(provider.lookUp("default").isPresent());
    }
}
