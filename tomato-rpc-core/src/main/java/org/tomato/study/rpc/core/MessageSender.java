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

package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.Command;

import java.util.concurrent.CompletableFuture;

/**
 * send rpc request
 * @author Tomato
 * Created on 2021.03.31
 */
public interface MessageSender {

    /**
     * send rpc request
     * @param msg request message
     * @return response message
     */
    CompletableFuture<Command> send(Command msg);

    /**
     * get the vip of the sender target server
     * @return vip
     */
    String getServiceVIP();
}
