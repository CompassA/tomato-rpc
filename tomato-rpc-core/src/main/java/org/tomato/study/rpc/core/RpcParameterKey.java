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

import org.tomato.study.rpc.core.data.ExtensionHeader;

/**
 * parameter key。
 * 由于parameter的value均为序列化后的String，为规范使用，请表明value类型
 * @author Tomato
 * Created on 2022.02.26
 */
public final class RpcParameterKey {
    /**
     * 超时时间
     * type: {@link Long}
     */
    public static final String TIMEOUT = "timeout";

    /**
     * 压缩
     * type: {@link Boolean}
     */
    public static final String COMPRESS = ExtensionHeader.COMPRESS.getName();
}
