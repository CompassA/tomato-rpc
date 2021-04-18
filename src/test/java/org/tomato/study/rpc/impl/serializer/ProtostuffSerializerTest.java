package org.tomato.study.rpc.impl.serializer;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.SerializerHolder;
import org.tomato.study.rpc.core.protocol.Command;
import org.tomato.study.rpc.core.protocol.CommandFactory;
import org.tomato.study.rpc.core.protocol.CommandModel;
import org.tomato.study.rpc.core.protocol.CommandType;
import org.tomato.study.rpc.core.protocol.Parameter;
import org.tomato.study.rpc.data.MethodContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public class ProtostuffSerializerTest {

    @Test
    public void test() {
        Map<Integer, Parameter> map = new HashMap<>();
        map.put(1, new Parameter("key", "val"));
        map.put(2, new Parameter("key2", "val2"));

        List<Parameter> list = new ArrayList<>();
        list.add(new Parameter("key", "val"));
        list.add(new Parameter("key2", "val2"));

        List<List<Parameter>> listList = new ArrayList<>();
        listList.add(list);

        MethodContext methodContext = MethodContext.builder()
                .interfaceName("c.s.f.wd.w.dw.d")
                .methodName("mockMethodName")
                .parameters(new Object[] { "mockString", 1, LocalDateTime.now(), listList, map})
                .build();
        Command mockCommand = CommandFactory.INSTANCE.createRequest(
                methodContext, list, SerializerHolder.getSerializer((byte) 0), CommandType.RPC_REQUEST);
        CommandModel<MethodContext> commandModel = CommandModel.toModel(mockCommand, MethodContext.class);
        Assert.assertNotNull(commandModel);
        Assert.assertEquals(2, commandModel.getExtension().size());
        Assert.assertTrue(commandModel.getExtension()
                .stream()
                .anyMatch(parameter -> parameter.getKey().equals("key")));
    }
}
