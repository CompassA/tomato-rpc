package org.tomato.study.rpc.core.data;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.api.TomatoApi;

import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.09.29
 */
public class ApiConfigTest {

    private static final String MOCK_VIP = "test";

    @Test
    public void test() {
        Assert.assertTrue(ApiConfig.create(StubFactory.class).isEmpty());
        Assert.assertTrue(ApiConfig.create(Object.class).isEmpty());
        Optional<ApiConfig<TestApi>> apiConfig = ApiConfig.create(TestApi.class);
        Assert.assertTrue(apiConfig.isPresent());
        Assert.assertSame(apiConfig.get().getApi(), TestApi.class);
        Assert.assertEquals(MOCK_VIP, apiConfig.get().getServiceVIP());
    }

    @TomatoApi(serviceVIP = MOCK_VIP)
    public static interface TestApi {

    }
}
