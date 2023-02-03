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

import org.tomato.study.rpc.core.spi.SpiInterface;

import java.util.List;

/**
 * serialization core method
 * @author Tomato
 * Created on 2021.03.31
 */
@SpiInterface("json")
public interface Serializer {

    /**
     * serialize
     * @param object to be serialized
     * @param <T> origin type
     * @return serialized data
     */
    <T> byte[] serialize(T object);

    /**
     * deserialize
     * @param data serialized data
     * @param clazz target type class instance
     * @param <T> target type
     * @return deserialized object
     */
    <T> T deserialize(byte[] data, Class<T> clazz);

    /**
     * describe the serializer index of serializer in command
     * @return serializer class
     */
    byte serializerIndex();

    /**
     * serialize list values
     * @param list list data
     * @param memberType list member type
     * @param <T> list member class instance
     * @return the serialized list data
     */
    default <T> byte[] serializeList(List<T> list, Class<T> memberType) {
        return serialize(list);
    }

    /**
     * deserialize list values
     * @param listData serialized list data
     * @param memberType list member type
     * @param <T> list member class instance
     * @return the deserialized list data
     */
    @SuppressWarnings("unchecked")
    default <T> List<T> deserializeList(byte[] listData, Class<T> memberType) {
        return (List<T>) deserialize(listData, List.class);
    }
}
