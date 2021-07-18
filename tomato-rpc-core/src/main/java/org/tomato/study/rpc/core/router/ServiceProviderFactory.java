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

package org.tomato.study.rpc.core.router;

import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * @author Tomato
 * Created on 2021.07.10
 */
@SpiInterface(
        paramName = "serviceProviderFactory",
        defaultSpiValue = "org.tomato.study.rpc.registry.zookeeper.impl.BalanceServiceProviderFactory"
)
public interface ServiceProviderFactory {

    /**
     * create service provider
     * @param vip provider vip
     * @return service provider
     */
    ServiceProvider create(String vip);
}