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

import org.apache.zookeeper.WatchedEvent;
import org.junit.After;
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
import org.tomato.study.rpc.core.router.ServiceProviderFactory;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.utils.ReflectUtils;
import org.tomato.study.rpc.zookeeper.CuratorClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
@RunWith(PowerMockRunner.class)
public class ListenerTest extends BaseTest {

    private final String mockVIP = "mock_vip";

    private PathChildrenWatcher watcher;

    private PathChildrenListener listener;

    private List<MetaData> mockChildren;

    private RpcInvokerFactory mockInvokerFactory;

    @Mock
    private ZookeeperRegistry mockRegistry;

    @Mock
    private CuratorClient mockClient;

    @Mock
    private WatchedEvent mockEvent;

    @Mock
    private SpiLoader<ServiceProviderFactory> mockProviderFactoryLoader;

    @Mock
    private SpiLoader<RpcInvokerFactory> mockInvokerFactoryLoader;


    @Before
    public void init() throws Exception {
        whenNew(CuratorClient.class).withAnyArguments().thenReturn(this.mockClient);
        this.mockRegistry = spy(
                new ZookeeperRegistry(
                        ZookeeperConfig.builder()
                                .connString("mock")
                                .namespace("tomato")
                                .charset(StandardCharsets.UTF_8)
                                .build()
                )
        );
        ReflectUtils.reflectSet(
                this.mockRegistry,
                ZookeeperRegistry.class,
                "curatorWrapper",
                mock(CuratorClient.class));
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
        this.listener = new PathChildrenListener(this.mockRegistry);
        this.watcher = PathChildrenWatcher.builder()
                .zkClient(this.mockClient)
                .childrenListener(this.listener)
                .build();
        this.mockChildren = new ArrayList<>(mockMetadataSet(mockVIP));
    }

    @After
    public void destroy() {
        this.watcher = null;
        this.listener = null;
        this.mockChildren = null;
        this.mockInvokerFactory = null;
    }

    @Test
    @PrepareForTest({SpiLoader.class})
    public void notifyTest() throws Exception {
        mockStatic(SpiLoader.class);
        when(SpiLoader.getLoader(eq(ServiceProviderFactory.class))).thenReturn(this.mockProviderFactoryLoader);
        when(this.mockProviderFactoryLoader.load()).thenReturn(new BalanceServiceProviderFactory());
        when(SpiLoader.getLoader(eq(RpcInvokerFactory.class))).thenReturn(this.mockInvokerFactoryLoader);
        when(this.mockInvokerFactoryLoader.load()).thenReturn(this.mockInvokerFactory);
        when(this.mockEvent.getPath()).thenReturn("/tomato/mock_vip/stage/providers");
        when(this.mockClient.getChildrenAndAddWatcher(any(), any())).thenReturn(
                mockChildren.stream()
                        .map(MetaData::convert)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(URI::toString)
                        .collect(Collectors.toList())
        );

        this.watcher.process(this.mockEvent);
        Assert.assertTrue(this.mockRegistry.lookup(mockVIP, "default").isPresent());

        ConcurrentMap<String, ServiceProvider> providerMap = ReflectUtils.reflectGet(
                this.mockRegistry, ZookeeperRegistry.class, "providerMap");
        Assert.assertEquals(1, providerMap.values().size());
        checkInvokerMap(providerMap.values().iterator().next(), mockChildren);
    }
}
