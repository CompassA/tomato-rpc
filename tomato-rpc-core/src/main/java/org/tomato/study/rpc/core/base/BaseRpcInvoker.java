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

package org.tomato.study.rpc.core.base;

import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.core.transport.RpcInvoker;

/**
 * @author Tomato
 * Created on 2021.11.26
 */
public abstract class BaseRpcInvoker implements RpcInvoker {

    /**
     * RPC服务节点的ip、端口等数据
     */
    protected final MetaData nodeInfo;

    /**
     * 客户端为请求body体设置的序列化方式
     */
    protected final Serializer commandSerializer;

    public BaseRpcInvoker(MetaData nodeInfo) {
        this.nodeInfo = nodeInfo;
        this.commandSerializer = SpiLoader.getLoader(Serializer.class).load();
    }

    @Override
    public String getGroup() {
        return nodeInfo.getGroup();
    }

    @Override
    public MetaData getMetadata() {
        return nodeInfo;
    }
}
