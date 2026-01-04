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

package org.tomato.study.rpc.netty.interceptor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tomato.study.rpc.core.RpcParameterKey;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.serializer.JsonSerializer;
import org.tomato.study.rpc.core.serializer.Serializer;
import org.tomato.study.rpc.core.utils.GzipUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2022.02.27
 */
@SuppressWarnings("all")
public class CompressInterceptorTest {

    private final CompressInterceptor compressInterceptor = new CompressInterceptor();
    private final Serializer jsonSerializer = new JsonSerializer();
    private final MockPOJO mockPOJO = new MockPOJO("1", "abc");
    private final Map<String, String> extensionHeaderMap = new HashMap(){{
        put(RpcParameterKey.COMPRESS, Boolean.TRUE.toString());
    }};
    private Command mockRequest = CommandFactory.request(mockPOJO, jsonSerializer, extensionHeaderMap, CommandType.RPC_REQUEST);
    private Command mockResponse = CommandFactory.response(mockRequest.getHeader().getId(), mockPOJO, jsonSerializer, CommandType.RPC_RESPONSE);

    @Before
    public void init() {
        CommandFactory.changeBody(mockRequest, GzipUtils.gzip(mockRequest.getBody()));
    }

    @Test
    public void functionTest() throws Exception {
        // 收到压缩的请求，进行解压
        Command newRequest = compressInterceptor.interceptRequest(mockRequest, extensionHeaderMap);
        Assert.assertTrue(mockPOJO.equals(
                jsonSerializer.deserialize(newRequest.getBody(), MockPOJO.class)));

        // 发送请求，进行压缩
        Command newResponse = compressInterceptor.postProcessResponse(mockRequest, mockResponse, extensionHeaderMap);
        Assert.assertTrue(mockPOJO.equals(
                jsonSerializer.deserialize(
                        GzipUtils.unGzip(mockResponse.getBody()),
                        MockPOJO.class)));

    }


    @Setter
    @Getter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class MockPOJO {
        public String id;
        public String name;
        public LocalDateTime date = LocalDateTime.now();
        public BigDecimal decimal = new BigDecimal("2123.2");

        public MockPOJO(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
