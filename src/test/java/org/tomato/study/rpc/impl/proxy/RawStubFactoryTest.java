package org.tomato.study.rpc.impl.proxy;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.NameService;

/**
 * @author Tomato
 * Created on 2021.04.03
 */
public class RawStubFactoryTest {

    @Test
    public void testCommon() throws Exception {
        RawStubFactory rawStubFactory = new RawStubFactory();
        NameService stubInstance = rawStubFactory.createStub(null, NameService.class);
        Assert.assertTrue(stubInstance.getClass().getName().contains("stub"));
    }
}
