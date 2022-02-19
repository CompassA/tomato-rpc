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

package org.tomato.study.rpc.core;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tomato-Rpc支持的键值对参数
 * @author Tomato
 * Created on 2021.11.07
 */
public final class RpcJvmConfigKey {

    /**
     * 多键值对分隔符
     */
    public static final String ENTRY_DELIMITER = "&";

    /**
     * 键值分隔符
     */
    public static final String KEY_VALUE_DELIMITER = ":";

    /**
     * 可在jvm参数中配置自身的环境
     */
    public static final String MICRO_SERVICE_STAGE = "tomato-rpc.service-stage";

    /**
     * 可在jvm参数中配置自身的服务版本
     */
    public static final String MICRO_SERVICE_GROUP = "tomato-rpc.service-group";

    /**
     * 可在jvm参数重配置感兴趣的服务的版本
     */
    public static final String MICRO_SUBSCRIBE_GROUP = "tomato-rpc.subscribed-services-group";

    /**
     * 注册中心地址
     */
    public static final String NAME_SERVICE_URI = "tomato-rpc.name-service-uri";

    /**
     * spi配置
     */
    public static final String SPI_CUSTOM_CONFIG = "tomato-rpc.spi";

    /**
     * 解析多重键值对参数
     * @param param key1:value1&key2:value2&key3:value3
     * @return 只读的键值对map
     */
    public static Map<String, String> parseMultiKeyValue(String param) {
        if (StringUtils.isBlank(param)) {
            return Collections.emptyMap();
        }
        Map<String, String> kvMap = new HashMap<>();
        for (String kv : param.split(ENTRY_DELIMITER)) {
            String[] kvSplitRes = kv.split(KEY_VALUE_DELIMITER);
            if (kvSplitRes.length == 2) {
                kvMap.put(kvSplitRes[0], kvSplitRes[1]);
            }
        }
        return Collections.unmodifiableMap(kvMap);
    }
}
