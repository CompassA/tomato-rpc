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

package org.tomato.study.rpc.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 流资源
 * @author Tomato
 * Created on 2021.11.15
 */
public interface StreamResource {

    /**
     * 获取一个输入流
     * @return 一个全新的输入流
     * @throws IOException 获取流异常
     */
    InputStream openNewStream() throws IOException;

    /**
     * 流资源是否存在
     * @return true 存在
     */
    boolean exists();

    /**
     * 是否可读
     * @return true 可读
     */
    boolean isReadable();

    /**
     * 获取资源的URL
     * @return URL
     */
    URL getURL();

}
