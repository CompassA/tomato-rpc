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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * json 序列化方式
 * @author Tomato
 * Created on 2021.10.03
 */
public class JsonSerializer implements Serializer {

    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper()
                // 设置时间格式
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
                // java8相关类型支持，如LocalDateTime
                .findAndRegisterModules()
                // 反序列化的时候如果多了其他属性,不抛出异常
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 如果是空对象的时候,不抛异常
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                // 反序列化时忽略null属性
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                // 设置可解析字段
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                // 将对象类型写进Json
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

    }

    @Override
    public <T> byte[] serialize(T object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_SERIALIZE_ERROR.create(
                String.format("json serialize failed: %s", object.getClass().getSimpleName())
            ));
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_SERIALIZE_ERROR.create(
                String.format("json deserialize failed: %s", clazz.getSimpleName())
            ));
        }
    }

    @Override
    public byte serializerIndex() {
        return 1;
    }

    @Override
    public <T> List<T> deserializeList(byte[] listData, Class<T> memberType) {
        try {
            return objectMapper.readValue(listData, new TypeReference<List<T>>() {});
        } catch (IOException e) {
            throw new TomatoRpcRuntimeException(TomatoRpcCoreErrorEnum.RPC_SERIALIZE_ERROR.create(
                String.format("json deserializeList failed: %s", memberType.getSimpleName())
            ));
        }
    }

//    用GSON，我暂时没找到能解决序列化Object[]对象类型丢失的问题，故废弃，转而使用Jackson
//    private final Gson gson = new GsonBuilder()
//            .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
//            .create();
//
//    @Override
//    public <T> byte[] serialize(T object) {
//        return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
//    }
//
//    @Override
//    public <T> T deserialize(byte[] data, Class<T> clazz) {
//        return gson.fromJson(new String(data, StandardCharsets.UTF_8), clazz);
//    }
//
//    @Override
//    public <T> byte[] serializeList(List<T> list, Class<T> memberType) {
//        return serialize(list);
//    }
//
//    @Override
//    public <T> List<T> deserializeList(byte[] listData, Class<T> memberType) {
//        return gson.fromJson(
//                new String(listData, StandardCharsets.UTF_8),
//                new ParameterizedType() {
//                    @Override
//                    public Type[] getActualTypeArguments() {
//                        return new Type[] { memberType };
//                    }
//
//                    @Override
//                    public Type getRawType() {
//                        return List.class;
//                    }
//
//                    @Override
//                    public Type getOwnerType() {
//                        return null;
//                    }
//                }
//        );
//    }
}
