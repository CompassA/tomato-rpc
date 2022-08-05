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

package org.tomato.study.rpc.core.loadbalance;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.LoadBalance;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.core.transport.RpcInvoker;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Tomato
 * Created on 2022.08.02
 */
public class LoadBalanceTest {

    @Test
    public void wrrLoadBalanceTest() {
        LoadBalance loadBalance = SpiLoader.getLoader(LoadBalance.class).load();
        Invocation invocationA = mock(Invocation.class);
        Invocation invocationB = mock(Invocation.class);
        RpcInvoker invoker1 = mock(RpcInvoker.class);
        RpcInvoker invoker2 = mock(RpcInvoker.class);
        RpcInvoker invoker3 = mock(RpcInvoker.class);
        RpcInvoker invoker4 = mock(RpcInvoker.class);
        RpcInvoker invoker5 = mock(RpcInvoker.class);
        RpcInvoker invoker6 = mock(RpcInvoker.class);

        URI nodeUrl1 = URI.create("tomato://127.0.0.1:1/?micro-service-id=mockIdA&stage=dev&group=test&property=weight:1");
        URI nodeUrl2 = URI.create("tomato://127.0.0.1:2/?micro-service-id=mockIdA&stage=dev&group=test&property=weight:1");
        URI nodeUrl3 = URI.create("tomato://127.0.0.1:3/?micro-service-id=mockIdA&stage=dev&group=test&property=weight:1");
        MetaData nodeInfo1 = MetaData.convert(nodeUrl1).get();
        MetaData nodeInfo2 = MetaData.convert(nodeUrl2).get();
        MetaData nodeInfo3 = MetaData.convert(nodeUrl3).get();
        List<RpcInvoker> invokerGroupA = Arrays.asList(invoker1, invoker2, invoker3);

        URI nodeUrl4 = URI.create("tomato://127.0.0.1:4/?micro-service-id=mockIdB&stage=dev&group=test&property=weight:4");
        URI nodeUrl5 = URI.create("tomato://127.0.0.1:5/?micro-service-id=mockIdB&stage=dev&group=test&property=weight:2");
        URI nodeUrl6 = URI.create("tomato://127.0.0.1:6/?micro-service-id=mockIdB&stage=dev&group=test&property=weight:1");
        MetaData nodeInfo4 = MetaData.convert(nodeUrl4).get();
        MetaData nodeInfo5 = MetaData.convert(nodeUrl5).get();
        MetaData nodeInfo6 = MetaData.convert(nodeUrl6).get();
        List<RpcInvoker> invokerGroupB = Arrays.asList(invoker4, invoker5, invoker6);


        when(invoker1.getMetadata()).thenReturn(nodeInfo1);
        when(invoker2.getMetadata()).thenReturn(nodeInfo2);
        when(invoker3.getMetadata()).thenReturn(nodeInfo3);
        when(invoker4.getMetadata()).thenReturn(nodeInfo4);
        when(invoker5.getMetadata()).thenReturn(nodeInfo5);
        when(invoker6.getMetadata()).thenReturn(nodeInfo6);
        when(invocationA.getApiId()).thenReturn("mockIdA");
        when(invocationB.getApiId()).thenReturn("mockIdB");


        List<MetaData> invokerGroupASelectResult = new ArrayList<>(0);
        List<MetaData> invokerGroupBSelectResult = new ArrayList<>(0);
        for (int i = 0; i < 100; ++i) {
            RpcInvoker targetA = loadBalance.select(invocationA, invokerGroupA);
            RpcInvoker targetB = loadBalance.select(invocationB, invokerGroupB);
            Assert.assertNotNull(targetA);
            Assert.assertNotNull(targetB);
            invokerGroupASelectResult.add(targetA.getMetadata());
            invokerGroupBSelectResult.add(targetB.getMetadata());
        }

        // 1 2 3 1 2 3 1 2 3 1 2 3
        Assert.assertEquals(invokerGroupASelectResult.get(0), nodeInfo1);
        Assert.assertEquals(invokerGroupASelectResult.get(1), nodeInfo2);
        Assert.assertEquals(invokerGroupASelectResult.get(2), nodeInfo3);
        Assert.assertEquals(invokerGroupASelectResult.get(3), nodeInfo1);
        Assert.assertEquals(invokerGroupASelectResult.get(4), nodeInfo2);
        Assert.assertEquals(invokerGroupASelectResult.get(5), nodeInfo3);
        Assert.assertEquals(invokerGroupASelectResult.get(6), nodeInfo1);
        Assert.assertEquals(invokerGroupASelectResult.get(7), nodeInfo2);
        Assert.assertEquals(invokerGroupASelectResult.get(8), nodeInfo3);


        // 4 5 4 6 4 5 4    4 5 4 6 4 5 4    4 5 4 6 4 5 4
        Assert.assertEquals(invokerGroupBSelectResult.get(0), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(1), nodeInfo5);
        Assert.assertEquals(invokerGroupBSelectResult.get(2), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(3), nodeInfo6);
        Assert.assertEquals(invokerGroupBSelectResult.get(4), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(5), nodeInfo5);
        Assert.assertEquals(invokerGroupBSelectResult.get(6), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(7), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(8), nodeInfo5);
        Assert.assertEquals(invokerGroupBSelectResult.get(9), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(10), nodeInfo6);
        Assert.assertEquals(invokerGroupBSelectResult.get(11), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(12), nodeInfo5);
        Assert.assertEquals(invokerGroupBSelectResult.get(13), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(14), nodeInfo4);
        Assert.assertEquals(invokerGroupBSelectResult.get(15), nodeInfo5);

    }
}
