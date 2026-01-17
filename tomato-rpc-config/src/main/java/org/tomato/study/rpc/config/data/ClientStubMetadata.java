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

package org.tomato.study.rpc.config.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.tomato.study.rpc.config.annotation.RpcClientStub;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * 客户端接口配置的元数据
 * @author Tomato
 * Created on 2021.11.21
 */
@Getter
@ToString
@AllArgsConstructor
public class ClientStubMetadata<T> {

    /**
     * rpc接口类型
     */
    private Class<T> stubClass;

    /**
     * 微服务id
     */
    private String microServiceId;

    /**
     * 客户端调用超时时间
     */
    private Long timeout;

    /**
     * 调用的分组
     */
    private String group;

    /**
     * 消息体是否开启压缩
     */
    private boolean compressBody;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientStubMetadata)) {
            return false;
        }
        ClientStubMetadata<?> that = (ClientStubMetadata<?>) o;
        return Objects.equals(stubClass, that.stubClass)
                && Objects.equals(microServiceId, that.microServiceId)
                && Objects.equals(timeout, that.timeout)
                && Objects.equals(group, that.group)
                && Objects.equals(compressBody, that.compressBody);
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode = hashCode * 31 + Objects.hashCode(stubClass);
        hashCode = hashCode * 31 + Objects.hashCode(microServiceId);
        hashCode = hashCode * 31 + Objects.hashCode(timeout);
        hashCode = hashCode * 31 + Objects.hashCode(group);
        hashCode = hashCode * 31 + Objects.hashCode(compressBody);
        return hashCode;
    }

    /**
     * 将一个成员字段配置的RpcClientStub信息提取出来
     * @param stubFiled 类字段
     * @return 配置的Stub信息
     */
    public static Optional<ClientStubMetadata<?>> create(Field stubFiled) {
        RpcClientStub rpcClientStub = stubFiled.getAnnotation(RpcClientStub.class);
        if (rpcClientStub == null) {
            return Optional.empty();
        }
        return create(rpcClientStub, stubFiled.getType());
    }

    /**
     * 将一个成员字段配置的RpcClientStub信息提取出来
     * @param rpcClientStub 类字段
     * @return 配置的Stub信息
     */
    public static Optional<ClientStubMetadata<?>> create(RpcClientStub rpcClientStub, Class<?> api) {
        if (!api.isInterface()) {
            return Optional.empty();
        }
        ClientStubMetadata<?> value = new ClientStubMetadata<>(
                api,
                rpcClientStub.microServiceId(),
                rpcClientStub.timeout(),
                rpcClientStub.group(),
                rpcClientStub.compressBody()
        );
        return Optional.of(value);
    }

    public String uniqueKey() {
        return new StringBuilder()
                .append(stubClass.getCanonicalName())
                .append("_")
                .append(microServiceId)
                .append("_")
                .append(timeout)
                .append("_")
                .append(group)
                .append("_")
                .append(compressBody)
                .toString();
    }
}
