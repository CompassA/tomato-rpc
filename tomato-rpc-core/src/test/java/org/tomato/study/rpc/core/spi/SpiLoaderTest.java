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

package org.tomato.study.rpc.core.spi;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceA;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceAImpl;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceAOtherImpl;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceBImpl;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceCImpl;
import org.tomato.study.rpc.core.spi.spitest.SpiInterfaceD;

import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.11.26
 */
public class SpiLoaderTest {

    @Test
    public void spiInitTest() {
        SpiLoader<SpiInterfaceA> loader = SpiLoader.getLoader(SpiInterfaceA.class);
        Assert.assertSame(loader.getSpiInterface(), SpiInterfaceA.class);
        Assert.assertTrue(loader.isSingletonInstance());
        Assert.assertEquals(loader.getDefaultKey(), SpiInterfaceA.class.getAnnotation(SpiInterface.class).value());
        Map<String, Class<? extends SpiInterfaceA>> componentMap = loader.getComponentMap();
        Assert.assertEquals(componentMap.values().size(), 2);
        Assert.assertTrue(componentMap.containsValue(SpiInterfaceAImpl.class));
        Assert.assertTrue(componentMap.containsValue(SpiInterfaceAOtherImpl.class));
    }

    /**
     * 测试循环依赖
     * {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceAImpl}
     * 依赖 {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceB}
     *
     * {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceBImpl}
     * 依赖 {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceC}
     *
     * {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceCImpl}
     * 依赖 {@link org.tomato.study.rpc.core.spi.spitest.SpiInterfaceA}
     */
    @Test
    public void loopDependencyTest() {
        SpiInterfaceA a = SpiLoader.getLoader(SpiInterfaceA.class).load();
        Assert.assertTrue(a instanceof SpiInterfaceAImpl);
        Assert.assertTrue(a.getB() instanceof SpiInterfaceBImpl);
        Assert.assertTrue(a.getB().getC() instanceof SpiInterfaceCImpl);
        Assert.assertTrue(a.getB().getC().getA() instanceof SpiInterfaceAImpl);
    }

    /**
     * 测试jvm配置spi
     */
    @Test
    public void jvmConfigTest() {
        String parameter = "org.tomato.study.rpc.core.spi.spitest.SpiInterfaceA:other";
        System.setProperty(RpcJvmConfigKey.SPI_CUSTOM_CONFIG, parameter);
        SpiLoader.resetPriorityMap();
        SpiInterfaceA component = SpiLoader.getLoader(SpiInterfaceA.class).load();
        Assert.assertTrue(component instanceof SpiInterfaceAOtherImpl);
    }
}
