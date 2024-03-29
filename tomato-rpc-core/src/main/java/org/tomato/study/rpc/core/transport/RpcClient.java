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

package org.tomato.study.rpc.core.transport;

import org.tomato.study.rpc.core.ResponseFuture;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.observer.LifeCycle;

/**
 * send rpc request
 * @param <T> 网络数据通信协议对象
 * @author Tomato
 * Created on 2021.03.31
 */
public interface RpcClient<T> extends LifeCycle {

    /**
     * send rpc request
     * @param msg request message
     * @throws TomatoRpcException message send exception
     * @return response message
     */
    ResponseFuture<T> send(T msg) throws TomatoRpcException;

    /**
     * get target host
     * @return host
     */
    String getHost();

    /**
     * get target port
     * @return port
     */
    int getPort();

    /**
     * 是否可用
     * @return true 可用
     */
    boolean isUsable();
}
