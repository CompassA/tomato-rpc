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

package org.tomato.study.rpc.core.circuit;

import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * 熔断器工厂
 * @author Tomato
 * Created on 2022.08.06
 */
@SpiInterface("default")
public interface CircuitBreakerFactory {

    /**
     * 创建熔断器
     * @param rpcConfig rpc配置
     * @return 熔断器
     */
    CircuitBreaker createBreaker(RpcConfig rpcConfig);
}
