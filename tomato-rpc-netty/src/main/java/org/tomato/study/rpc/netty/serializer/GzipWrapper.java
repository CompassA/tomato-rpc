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

package org.tomato.study.rpc.netty.serializer;

import org.tomato.study.rpc.core.Serializer;
import org.tomato.study.rpc.netty.utils.GzipUtils;

import java.util.List;

/**
 * gzip 包装器
 * @author Tomato
 * Created on 2021.10.04
 */
public class GzipWrapper implements Serializer {

    private final Serializer serializer;

    public GzipWrapper(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public <T> byte[] serialize(T object) {
        return GzipUtils.gzip(
                serializer.serialize(object)
        );
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return serializer.deserialize(
                GzipUtils.unGzip(data),
                clazz
        );
    }

    @Override
    public byte serializerIndex() {
        return serializer.serializerIndex();
    }

    @Override
    public <T> byte[] serializeList(List<T> list, Class<T> memberType) {
        return GzipUtils.gzip(
                serializer.serializeList(list, memberType)
        );
    }

    @Override
    public <T> List<T> deserializeList(byte[] listData, Class<T> memberType) {
        return serializer.deserializeList(
                GzipUtils.unGzip(listData),
                memberType
        );
    }
}
