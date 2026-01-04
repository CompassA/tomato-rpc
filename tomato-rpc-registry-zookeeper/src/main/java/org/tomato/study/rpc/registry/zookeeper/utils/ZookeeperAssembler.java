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

package org.tomato.study.rpc.registry.zookeeper.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.dashboard.data.RpcRouterData;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.serializer.JsonSerializer;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2026.01.03
 */
public class ZookeeperAssembler {

    public static Charset CHARSET = StandardCharsets.UTF_8;
    public static final String PATH_DELIMITER = "/";
    public static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();

    /**
     * 一个服务的所有实例的上级文件夹: /namespace/micro-service-id/stage/PROVIDER_DICTIONARY
     */
    public static final String PROVIDER_DICTIONARY = "providers";

    /**
     * 一个服务的所有路由规则的上级文件夹 /namespace/micro-service-id/stage/ROUTER_DICTIONARY/target-micro-service-id
     */
    public static final String ROUTER_DICTIONARY = "routers";

    /**
     * 拼接单个微服务在Zookeeper的目录
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @return 路径
     */
    public static String buildServiceNodePath(String microServiceId, String stage, URI invokerURI) {
        return convertToZNodePath(microServiceId, stage, PROVIDER_DICTIONARY, invokerURI.toString());
    }

    /**
     * 拼接单个微服务在Zookeeper的目录
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @return 路径
     */
    public static String buildServiceNodeParent(String microServiceId, String stage) {
        return convertToZNodePath(microServiceId, stage, PROVIDER_DICTIONARY);
    }

    /**
     * 拼接单个微服务在Zookeeper的路由规则目录
     * @param microServiceId 微服务ID
     * @param stage 环境
     * @param routerMicroServiceId 路由规则路由目标的微服务ID
     * @return 路径
     */
    public static String buildServiceRouterPath(String microServiceId, String stage, String routerMicroServiceId) {
        return convertToZNodePath(microServiceId, stage, ROUTER_DICTIONARY, routerMicroServiceId);
    }

    /**
     * 将zookeeperURL 转换为 内存对象
     * @param invokerURL zookeeper中保存的URL
     * @return invoker元数据内存对象
     */
    public static Optional<MetaData> convertToModel(String invokerURL) {
        String decode = URLDecoder.decode(invokerURL, CHARSET);
        URI uri = URI.create(decode);
        return MetaData.convert(uri);
    }

    /**
     * 将传入的字符串拼接成zookeeper路径
     * @param parts 多个字符串
     * @return zookeeper路径
     */
    private static String convertToZNodePath(String... parts) {
        if (parts == null || parts.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder(0);
        for (String part : parts) {
            builder.append(PATH_DELIMITER)
                .append(URLEncoder.encode(part, CHARSET));
        }
        return builder.toString();
    }

    public static RpcRouterData toRouterData(byte[] rawRouterData) {
        return JSON_SERIALIZER.deserialize(rawRouterData, RpcRouterData.class);
    }


    public static byte[] toRouterBytes(RpcRouterData routerData) throws JsonProcessingException {
        return JSON_SERIALIZER.serialize(routerData);
    }
}
