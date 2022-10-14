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

import org.tomato.study.rpc.core.ServerHandler;
import org.tomato.study.rpc.core.data.Command;
import org.tomato.study.rpc.core.data.CommandFactory;
import org.tomato.study.rpc.core.data.CommandType;
import org.tomato.study.rpc.core.data.Header;
import org.tomato.study.rpc.core.serializer.SerializerHolder;

/**
 * 服务端回应KeepAlive
 * @author Tomato
 * Created on 2021.10.05
 */
public class KeepAliveRequestHandler implements ServerHandler {

    @Override
    public Command handle(Command command) throws Exception {
        // 响应心跳信息
        Header header = command.getHeader();
        if (header == null) {
            return null;
        }
        return CommandFactory.response(
                header.getId(),
                null,
                SerializerHolder.getSerializer(header.getSerializeType()),
                CommandType.KEEP_ALIVE_RESPONSE
        );
    }

    @Override
    public CommandType getType() {
        return CommandType.KEEP_ALIVE_REQUEST;
    }
}
