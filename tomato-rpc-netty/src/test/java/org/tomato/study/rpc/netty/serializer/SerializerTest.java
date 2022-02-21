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

package org.tomato.study.rpc.netty.serializer;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandModel;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.Parameter;
import org.tomato.study.rpc.netty.codec.NettyCommandCodec;
import org.tomato.study.rpc.netty.data.RpcRequestDTO;

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

    @Test
    public void testProtostuff() {
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
        Command mockCommand = CommandFactory.request(
                rpcRequest, list, SerializerHolder.getSerializer((byte) 0), CommandType.RPC_REQUEST);
        CommandModel<RpcRequestDTO> commandModel = NettyCommandCodec.toModel(mockCommand, RpcRequestDTO.class);
        Assert.assertNotNull(commandModel);
        Assert.assertEquals(2, commandModel.getExtension().size());
        for (int i = 0; i < commandModel.getBody().getArgs().length; i++) {
            Assert.assertSame(rpcRequest.getArgs()[i].getClass(), commandModel.getBody().getArgs()[i].getClass());
        }
        Assert.assertTrue(commandModel.getExtension()
                .stream()
                .anyMatch(parameter -> parameter.getKey().equals("key")));
    }

    @Test
    public void testJson() {
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

        Command mockCommand = CommandFactory.request(
                rpcRequest, list, SerializerHolder.getSerializer((byte) 1), CommandType.RPC_REQUEST);
        CommandModel<RpcRequestDTO> commandModel = NettyCommandCodec.toModel(mockCommand, RpcRequestDTO.class);
        Assert.assertNotNull(commandModel);
        Assert.assertEquals(2, commandModel.getExtension().size());
        for (int i = 0; i < commandModel.getBody().getArgs().length; i++) {
            Assert.assertSame(rpcRequest.getArgs()[i].getClass(), commandModel.getBody().getArgs()[i].getClass());
        }
        Assert.assertTrue(commandModel.getExtension()
                .stream()
                .anyMatch(parameter -> parameter.getKey().equals("key")));
    }
}
