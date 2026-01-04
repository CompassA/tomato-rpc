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
package org.tomato.study.rpc.registry.zookeeper;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.registry.zookeeper.impl.BaseTest;
import org.tomato.study.rpc.registry.zookeeper.utils.ZookeeperAssembler;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 * @author Tomato
 * Created on 2022.02.17
 */
public class ZookeeperNameServerTest extends BaseTest {
    private TestingServer testServer;
    private int port = 5422;
    private String namespace = "tomato";
    private NameServerConfig config;
    private ZookeeperNameServer nameServer;
    private CuratorClient curatorClient;

    @Before
    public void init() throws Exception {
        testServer = new TestingServer(port);
        testServer.start();
        config = NameServerConfig.builder()
                .connString("127.0.0.1:" + port)
                .stage(BaseTest.stage)
                .build();
        nameServer = new ZookeeperNameServer(config);
        nameServer.init();
        nameServer.start();
        curatorClient = new CuratorClient(
                ZookeeperConfig.builder()
                        .namespace(namespace)
                        .connString(config.connString())
                        .build()
        ).start();
    }

    @After
    public void destroy() throws IOException, TomatoRpcException {
        curatorClient.close();
        curatorClient.close();
        nameServer.stop();
    }

    /**
     * 测试注册、取消注册
     */
    @Test
    public void nameServerTest() throws Exception {
        String serviceId = "test-service";
        Set<MetaData> invokerInfoSet = mockMetadataSet(serviceId);
        for (MetaData info : invokerInfoSet) {
            nameServer.registerService(info);
            Optional<URI> convertResult = MetaData.convert(info);
            if (convertResult.isEmpty()) {
                Assert.fail();
            }
            String path = ZookeeperAssembler.buildServiceNodePath(
                    info.getMicroServiceId(),
                    info.getStage(),
                    convertResult.get());
            Assert.assertNotNull(curatorClient.checkExists(path));
            nameServer.unregisterService(info);
            Assert.assertNull(curatorClient.checkExists(path));
        }
    }
}
