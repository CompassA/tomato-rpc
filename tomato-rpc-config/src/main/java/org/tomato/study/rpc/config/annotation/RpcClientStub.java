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

package org.tomato.study.rpc.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 客户端stub配置
 * @author Tomato
 * Created on 2021.11.18
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface RpcClientStub {

    /**
     * 用于设置服务的唯一标识
     * @return 服务唯一标识
     */
    String microServiceId();

    /**
     * 配置客户端调用的超时时间
     * @return 超时时间，单位ms
     */
    long timeout() default 5000;

    /**
     * 要调用哪个分组的api，如果没有配置，调用与自身一致的分组
     * @return 分组
     */
    String group() default "";

    /**
     * 发送消息时是否压缩消息体
     * @return true 压缩
     */
    boolean compressBody() default false;

}
