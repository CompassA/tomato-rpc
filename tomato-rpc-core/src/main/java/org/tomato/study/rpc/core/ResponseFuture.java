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

package org.tomato.study.rpc.core;

import java.util.concurrent.CompletableFuture;

/**
 * 一次网络通信后返回的数据
 * @author Tomato
 * Created on 2021.11.28
 */
public interface ResponseFuture<T> {

    /**
     * 获取消息id
     * @return 消息id
     */
    long getMessageId();

    /**
     * 获取响应Future
     * @return 响应
     */
    CompletableFuture<T> getFuture();

    /**
     * 销毁响应数据
     */
    void destroy();
}
