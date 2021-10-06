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

package org.tomato.study.rpc.netty.transport.handler;

import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 客户端空闲检测
 * @author Tomato
 * Created on 2021.10.05
 */
public class ClientIdleCheckHandler extends IdleStateHandler {

    /**
     * 连接在配置的时间段内无写操作时，触发空闲事件
     * @param writerIdleTime 配置的空闲时间
     */
    public ClientIdleCheckHandler(long writerIdleTime) {
        super(0, writerIdleTime, 0, TimeUnit.MILLISECONDS);
    }

}
