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

import java.io.Closeable;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface RpcServer extends Closeable {

    /**
     * start rpc sever
     * @return true: start the server
     */
    boolean start();


    /**
     * get rpc server ip
     * @return local host
     */
    String getHost();

    /**
     * get rpc server port
     * @return port
     */
    int getPort();

    /**
     * is rpc server closed
     * @return true: closed
     */
    boolean isClosed();
}
