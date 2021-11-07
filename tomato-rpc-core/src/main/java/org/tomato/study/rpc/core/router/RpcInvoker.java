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

import org.tomato.study.rpc.core.Invocation;
import org.tomato.study.rpc.core.Response;
import org.tomato.study.rpc.core.Result;
import org.tomato.study.rpc.core.data.MetaData;

import java.io.Closeable;

/**
 * divide invoker in the interface dimension
 * @author Tomato
 * Created on 2021.07.07
 */
public interface RpcInvoker extends Closeable {

    /**
     * get invoker group {@link MetaData#getGroup()}
     * @return invoker id
     */
    String getGroup();

    /**
     * get invoker metadata
     * @return invoker metadata
     */
    MetaData getMetadata();

    /**
     * invoke rpc method
     * @param invocation procedure call data
     * @return result
     */
    Result<Response> invoke(Invocation invocation);
}
