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

package org.tomato.study.rpc.core.serializer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.InvocationContext;
import org.tomato.study.rpc.core.data.Parameter;
import org.tomato.study.rpc.core.data.RpcRequestDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public class SerializerTest {

    @Before
    public void setUp() {
        InvocationContext.createIfAbsent();
    }

    @After
    public void tearDown() {
        InvocationContext.remove();
    }

    @Test
    public void testProtostuff() {
        RpcRequestDTO rpcRequest = createRpcRequest();
        Serializer serializer = SerializerHolder.getSerializer((byte) 0);
        Command mockCommand = CommandFactory.request(
                rpcRequest, serializer, InvocationContext.get(), CommandType.RPC_REQUEST);
        RpcRequestDTO deserialize = serializer.deserialize(mockCommand.getBody(), RpcRequestDTO.class);
        Assert.assertNotNull(mockCommand);
        Assert.assertEquals(rpcRequest, deserialize);
    }

    @Test
    public void testJson() {
        RpcRequestDTO rpcRequest = createRpcRequest();
        Serializer serializer = SerializerHolder.getSerializer((byte) 1);
        Command mockCommand = CommandFactory.request(
                rpcRequest, serializer,
                InvocationContext.get(),
                CommandType.RPC_REQUEST);
        RpcRequestDTO deserialize = serializer.deserialize(mockCommand.getBody(), RpcRequestDTO.class);
        // 坑的点，Map<Integer, xxx> 序列化反序列化后会变成 Map<String, xxxx>
        Assert.assertFalse(rpcRequest.equals(deserialize));
        deserialize.getArgs()[4] = rpcRequest.getArgs()[4] = null;
        Assert.assertTrue(rpcRequest.equals(deserialize));
    }

    private RpcRequestDTO createRpcRequest() {
        Map<Integer, Parameter> map = new HashMap<>();
        map.put(1, new Parameter("key", "val"));
        map.put(2, new Parameter("key2", "val2"));

        List<Parameter> list = new ArrayList<>();
        list.add(new Parameter("key", "val"));
        list.add(new Parameter("key2", "val2"));

        List<List<Parameter>> listList = new ArrayList<>();
        listList.add(list);

        RpcRequestDTO rpcRequest = RpcRequestDTO.builder()
                .interfaceName("c.s.f.wd.w.dw.d")
                .methodName("mockMethodName")
                .args(new Object[] { "mockString", 1, LocalDateTime.now(), listList, map})
                .build();
        InvocationContext.put("key3", "val3");
        InvocationContext.put("key3", "val3");
        return rpcRequest;
    }
}
