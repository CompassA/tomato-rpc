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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.registry.zookeeper.CuratorClient;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.utils.ReflectUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author Tomato
 * Created on 2021.07.18
 */
@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
public class ListenerTest extends BaseTest {

    private final String microServiceId = "mock_service_id";

    private final String stage = "default";

    private PathChildrenListener listener;

    private List<MetaData> mockChildren;

    private ZookeeperRegistry spyRegistry;

    @Mock
    private CuratorClient mockClient;

    @Mock
    private WatchedEvent mockEvent;

    @Before
    public void init() throws Exception {
        whenNew(CuratorClient.class).withAnyArguments().thenReturn(mockClient);
        spyRegistry = spy(
                new ZookeeperRegistry(
                        ZookeeperConfig.builder()
                                .connString("mock")
                                .namespace("tomato")
                                .charset(StandardCharsets.UTF_8)
                                .build()));
        ReflectUtils.reflectSet(
                spyRegistry,
                ZookeeperRegistry.class,
                "curatorWrapper",
                mock(CuratorClient.class));
        this.listener = new PathChildrenListener(spyRegistry, mockClient);
        this.mockChildren = new ArrayList<>(mockMetadataSet(microServiceId));
    }

    @After
    public void destroy() {
        this.listener = null;
        this.mockChildren = null;
    }

    @Test
    public void watcherProcessTest() throws Exception {
        // mock更新路径
        String mockPath = "/tomato/" + microServiceId + "/" + stage + "/providers";
        when(mockEvent.getPath()).thenReturn(mockPath);
        // mock路径对应的孩子节点
        Collection<URI> uriList = mockChildren.stream()
                .map(MetaData::convert)
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());
        List<String> children = uriList.stream()
                .map(URI::toString)
                .collect(Collectors.toList());
        when(mockClient.getChildrenAndAddWatcher(any(), any())).thenReturn(children);

        listener.process(this.mockEvent);

        verify(spyRegistry, times(1)).notify(
                eq(mockPath),
                eq(uriList)
        );
    }
}
