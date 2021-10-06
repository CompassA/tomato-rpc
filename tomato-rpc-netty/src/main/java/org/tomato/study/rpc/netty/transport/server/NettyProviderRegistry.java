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

package org.tomato.study.rpc.netty.transport.server;

import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.ProviderRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class NettyProviderRegistry implements ProviderRegistry {

    /**
     * server provider map:
     * id generated by this#providerId -> provider instance
     */
    private final ConcurrentMap<String, Object> providerMap = new ConcurrentHashMap<>(0);

    @Override
    public <T> void register(String vip, T instance, Class<T> providerInterface) {
        if (StringUtils.isBlank(vip) || instance == null || !providerInterface.isInterface()) {
            throw new IllegalCallerException("register invalid data");
        }
        this.providerMap.put(providerId(providerInterface.getName(), vip), instance);
    }

    @Override
    public Object getProvider(String vip, Class<?> providerInterface) {
        if (providerInterface == null || StringUtils.isBlank(vip)) {
            return null;
        }
        String providerId = providerId(providerInterface.getCanonicalName(), vip);
        return this.providerMap.get(providerId);
    }

    private String providerId(String interfaceName, String vip) {
        return interfaceName + "$" + vip;
    }
}