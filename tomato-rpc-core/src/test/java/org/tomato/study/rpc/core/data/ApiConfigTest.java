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

package org.tomato.study.rpc.core.data;

import org.junit.Assert;
import org.junit.Test;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.api.TomatoApi;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.09.29
 */
public class ApiConfigTest {

    private static final String MOCK_SERVICE_ID = "test";

    @Test
    public void test() {
        long timeoutMs = 30000;
        Assert.assertTrue(ApiConfig.create(StubFactory.class, timeoutMs).isEmpty());
        Assert.assertTrue(ApiConfig.create(Object.class, timeoutMs).isEmpty());
        Optional<ApiConfig<TestApi>> apiConfig = ApiConfig.create(TestApi.class, timeoutMs);
        Assert.assertTrue(apiConfig.isPresent());
        ApiConfig<TestApi> testApiApiConfig = apiConfig.get();
        Assert.assertSame(testApiApiConfig.getApi(), TestApi.class);
        Assert.assertEquals(MOCK_SERVICE_ID, testApiApiConfig.getMicroServiceId());
        Assert.assertTrue(Objects.equals(timeoutMs, testApiApiConfig.getTimeoutMs()));
    }

    @TomatoApi(microServiceId = MOCK_SERVICE_ID)
    public static interface TestApi {
    }
}
