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

package org.tomato.study.rpc.core.serializer;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 所有序列化器单例
 * @author Tomato
 * Created on 2021.04.01
 */
@Slf4j
public final class SerializerHolder {

    /**
     * 序列化器id -> 序列化器
     */
    private static final ConcurrentMap<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static {
        ServiceLoader.load(Serializer.class).forEach(
                serializer -> SERIALIZER_MAP.put(serializer.serializerIndex(), serializer));
    }

    public static Serializer getSerializer(final byte key) {
        return SERIALIZER_MAP.get(key);
    }
}
