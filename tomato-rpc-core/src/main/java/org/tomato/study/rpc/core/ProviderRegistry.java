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

package org.tomato.study.rpc.core;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface ProviderRegistry {

    /**
     * register provider handler
     * @param microServiceId micro-service-id
     * @param instance provider instance
     * @param providerInterface service provider interface
     * @param <T> provider interface type
     */
    <T> void register(String microServiceId, T instance, Class<T> providerInterface);

    /**
     * get provider instance
     * @param microServiceId micro service id
     * @param providerInterface service provider interface
     * @return provider instance
     */
    Object getProvider(String microServiceId, Class<?> providerInterface);
}
