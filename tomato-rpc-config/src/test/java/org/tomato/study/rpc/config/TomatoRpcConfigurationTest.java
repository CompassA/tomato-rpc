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

package org.tomato.study.rpc.config;

import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.tomato.study.rpc.config.component.TomatoRpcConfiguration;
import org.tomato.study.rpc.config.data.TomatoRpcProperties;
import org.tomato.study.rpc.config.test.TestClientBean;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.utils.ReflectUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Tomato
 * Created on 2021.11.19
 */
@SpringBootTest(classes = TomatoRpcConfigurationTest.class)
@ComponentScan(basePackages = {
        "org.tomato.study.rpc.config.component",
        "org.tomato.study.rpc.config.test"
})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SuppressWarnings("all")
public class TomatoRpcConfigurationTest {

    private static TestingServer TEST_SERVER;
    private static RpcCoreService RPC_CORE_SERVICE;

    @Autowired
    private TomatoRpcConfiguration configuration;

    @Autowired
    private TestClientBean testClientBean;

    @Autowired
    private RpcCoreService rpcCoreService;

    @PostConstruct
    public void postConstruct() {
        RPC_CORE_SERVICE = rpcCoreService;
    }

    @BeforeClass
    public static void init() throws Exception {
        TEST_SERVER = new TestingServer(3689);
        TEST_SERVER.start();
    }

    @AfterClass
    public static void destroy() throws IOException, TomatoRpcException {
        RPC_CORE_SERVICE.stop();
        TEST_SERVER.stop();
        TEST_SERVER.close();
    }

    @Test
    public void configurationTest() {
        TomatoRpcProperties properties = ReflectUtils.reflectGet(
                configuration, TomatoRpcConfiguration.class, "properties");
        Assert.assertNotNull(properties);
        Assert.assertEquals(properties.getMicroServiceId(), "rpc-test-service");
        Assert.assertEquals(properties.getSubscribedServices().size(), 3);
        Assert.assertEquals(properties.getSubscribedServices().get(0), "mock-service-a");
        Assert.assertEquals(properties.getSubscribedServices().get(1), "mock-service-b");
        Assert.assertEquals(properties.getSubscribedServices().get(2), "rpc-test-service");
        Assert.assertEquals(properties.getNameServiceUri(), "127.0.0.1:" + TEST_SERVER.getPort());
        Assert.assertTrue(Objects.equals(properties.getPort(), 7854));
        Assert.assertTrue(Objects.equals(properties.getBusinessThread(), 4));
        Assert.assertTrue(Objects.equals(properties.getStage(), "dev"));
        Assert.assertTrue(Objects.equals(properties.getGroup(), "main"));
        Assert.assertTrue(Objects.equals(properties.getServerIdleCheckMs(), 600000L));
        Assert.assertTrue(Objects.equals(properties.getClientKeepAliveMs(), 200000L));
        Assert.assertTrue(properties.isEnableCircuit());
        Assert.assertEquals(properties.getCircuitOpenRate().intValue(), 74);
        Assert.assertEquals(properties.getCircuitOpenSeconds().longValue(), 59L);
        Assert.assertEquals(properties.getCircuitWindow().intValue(), 98);
    }

    @Test
    public void beanRegisterTest() {
        String echoStr = "echo";
        Assert.assertEquals(echoStr, testClientBean.getTestApi().echo(echoStr));
    }

    @Test
    public void annotationTimeoutConfigTest() {
        boolean hasTimeout = false;
        try {
            testClientBean.getTimeoutApi().echo("");
        } catch (TomatoRpcRuntimeException e) {
            hasTimeout = Objects.equals(
                    e.getErrorInfo().getCode(),
                    TomatoRpcCoreErrorEnum.RPC_CLIENT_TIMEOUT.getCode());
        }
        Assert.assertTrue(hasTimeout);
    }
}
