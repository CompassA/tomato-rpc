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

import org.tomato.study.rpc.core.data.Command;

import java.util.Map;

/**
 * 协议拦截器
 * @author Tomato
 * Created on 2022.02.27
 */
public interface CommandInterceptor {

    /**
     * 对收到的消息做拦截处理
     * @param command 协议消息
     * @param extensionHeaders 解析过的拓展头部信息
     * @throws Exception 处理时发生的异常
     * @return 处理后的消息
     */
    Command interceptRequest(Command command, Map<String, String> extensionHeaders) throws Exception;

    /**
     * 对发送的消息做后置处理
     * @param request 收到的协议消息
     * @param response 发送的协议消息
     * @param extensionHeaders 收到的协议消息的拓展头部信息
     * @throws Exception 处理时发生的异常
     * @return 处理后的消息
     */
    Command postProcessResponse(Command request, Command response, Map<String, String> extensionHeaders) throws Exception;
}
