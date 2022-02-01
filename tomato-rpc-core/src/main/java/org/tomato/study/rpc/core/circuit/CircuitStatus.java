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

/**
 * @author Tomato
 * Created on 2021.12.03
 */
public interface CircuitStatus {

    /**
     * 获取当前熔断状态
     * @return 枚举状态码
     */
    int getState();

    /**
     * 获取失败率
     * @return 失败率
     */
    double failureRate();
}
