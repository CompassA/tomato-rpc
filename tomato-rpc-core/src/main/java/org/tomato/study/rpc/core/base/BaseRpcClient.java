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

import org.tomato.study.rpc.core.RpcClient;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

import java.net.URI;

/**
 * @author Tomato
 * Created on 2021.11.27
 */
public abstract class BaseRpcClient extends BaseLifeCycleComponent implements RpcClient {

    /**
     * RPC服务节点的ip、端口，URI形式
     */
    private final URI uri;

    public BaseRpcClient(URI uri) {
        this.uri = uri;
    }

    @Override
    public String getHost() {
        return uri.getHost();
    }

    @Override
    public int getPort() {
        return uri.getPort();
    }
}
