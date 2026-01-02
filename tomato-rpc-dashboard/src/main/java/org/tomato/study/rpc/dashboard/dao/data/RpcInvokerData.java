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

package org.tomato.study.rpc.dashboard.dao.data;

import lombok.Getter;
import lombok.Setter;
import org.tomato.study.rpc.core.data.MetaData;

/**
 * 一个rpc节点的数据
 * @author Tomato
 * Created on 2022.08.07
 */
@Getter
@Setter
public class RpcInvokerData {

    /**
     * 微服务配置
     */
    private String microServiceId;

    /**
     * 应用属性
     */
    private MetaData nodeProperties;

}
