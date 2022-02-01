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

import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * 熔断器
 * @author Tomato
 * Created on 2021.12.03
 */
@SpiInterface(value = "default", singleton = false)
public interface CircuitBreaker {

    /**
     * 关闭状态
     */
    int CLOSE = 1;

    /**
     * 熔断半开启
     */
    int HALF_OPEN = 2;

    /**
     * 熔断开启
     */
    int OPEN = 3;

    /**
     * 请求是否放行
     * @return true 放行; false 快速失败
     */
    boolean allow();

    /**
     * 增加成功请求数
     */
    void addSuccess();

    /**
     * 增加失败请求数
     */
    void addFailure();

    /**
     * 重置熔断阈值
     * @param threshold 失败率超过这个比例即熔断, 范围[0, 100]
     *                  ex: 20, 代表失败率超过20%即熔断
     */
    void resetThresholdRate(int threshold);

    /**
     * 重置熔断时间
     * @param periodNano 时间间隔，单位ns
     */
    void resetPeriod(long periodNano);

    /**
     * 获取当前熔断器数据
     * @return 数据
     */
    CircuitStatus getStatus();
}
