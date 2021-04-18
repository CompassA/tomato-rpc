package org.tomato.study.rpc.core;

import org.junit.Assert;
import org.tomato.study.rpc.core.test.TestService;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class RpcTest {

    private RpcCoreService rpcCoreService;

    /**
     * todo
     */
    public void executionChainTest() {
        TestService serverService = nums -> nums.stream().reduce(0, Integer::sum);
        String serviceVIP = "serviceService";
        URI serviceURI = rpcCoreService.registerProvider(serviceVIP, serverService, TestService.class);
        TestService clientStub = rpcCoreService.createStub(serviceVIP, TestService.class);
        List<Integer> testList = Arrays.asList(1, 2, 3, 4);

        Assert.assertEquals(clientStub.sum(testList), serverService.sum(testList));
    }


}
