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

package org.tomato.study.rpc.config.component;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.tomato.study.rpc.config.data.TomatoRpcProperties;
import org.tomato.study.rpc.utils.ReflectUtils;

import java.util.Objects;

/**
 * @author Tomato
 * Created on 2021.11.19
 */
@SpringBootTest(classes = TomatoRpcConfigurationTest.class)
@ComponentScan(basePackages = {
      "org.tomato.study.rpc.config.component"
})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SuppressWarnings("all")
public class TomatoRpcConfigurationTest {

    @Autowired
    private TomatoRpcConfiguration configuration;

    @Test
    public void configurationTest() {
        TomatoRpcProperties properties = ReflectUtils.reflectGet(
                configuration, TomatoRpcConfiguration.class, "properties");
        Assert.assertNotNull(properties);
        Assert.assertEquals(properties.getMicroServiceId(), "rpc-test-service");
        Assert.assertEquals(properties.getSubscribedServices().size(), 2);
        Assert.assertEquals(properties.getSubscribedServices().get(0), "mock-service-a");
        Assert.assertEquals(properties.getSubscribedServices().get(1), "mock-service-b");
        Assert.assertEquals(properties.getNameServiceUri(), "127.0.0.1:2181");
        Assert.assertTrue(Objects.equals(properties.getPort(), 34000));
        Assert.assertTrue(Objects.equals(properties.getBusinessThread(), 4));
        Assert.assertTrue(Objects.equals(properties.getStage(), "dev"));
        Assert.assertTrue(Objects.equals(properties.getGroup(), "main"));
        Assert.assertTrue(Objects.equals(properties.getServerIdleCheckMs(), 600000L));
        Assert.assertTrue(Objects.equals(properties.getClientKeepAliveMs(), 200000L));
        Assert.assertTrue(properties.isUseGzip());
    }
}
