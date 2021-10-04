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

package org.tomato.study.rpc.netty.serializer;

import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.Serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * 给所有序列化器添加包装器
     * @param wrapperClazz 包装器
     */
    public static void configWrapper(Class<? extends Serializer> wrapperClazz) {
        try {
            final Constructor<? extends Serializer> constructor = wrapperClazz.getConstructor(Serializer.class);
            final List<Serializer> wrapperList = new ArrayList<>(0);
            for (Serializer value : SERIALIZER_MAP.values()) {
                wrapperList.add(constructor.newInstance(value));
            }
            for (Serializer wrapper : wrapperList) {
                SERIALIZER_MAP.put(wrapper.serializerIndex(), wrapper);
            }
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }
}
