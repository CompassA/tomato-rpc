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

import org.tomato.study.rpc.core.data.MetaData;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * 代表一个微服务对象
 * @author Tomato
 * Created on 2021.07.07
 */
public interface MicroServiceSpace extends Closeable {

    /**
     * 微服务唯一标识
     * ex. "org.study.rpc.service"
     * @return vip
     */
    String getVIP();

    /**
     * 根据微服务版本找到匹配的一个微服务调用实例
     * @param version service version
     * @return invoker
     */
    Optional<RpcInvoker> lookUp(String version);

    /**
     * 刷新节点信息
     * @param metadataSet all rpc node metadata of a provider with same vip and stage
     * @throws IOException IO Exception during refresh invoker data
     */
    void refresh(Set<MetaData> metadataSet) throws IOException;

}
