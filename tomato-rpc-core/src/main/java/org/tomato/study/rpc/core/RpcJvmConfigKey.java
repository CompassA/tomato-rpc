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

/**
 * @author Tomato
 * Created on 2021.11.07
 */
public final class RpcJvmConfigKey {
    /**
     * 可在jvm参数中配置自身的服务版本
     */
    public static final String MICRO_SERVICE_GROUP = "tomato.service-group";


    /**
     * 可在jvm参数重配置感兴趣的服务的版本
     */
    public static final String MICRO_SUBSCRIBE_GROUP = "tomato.subscribed-service-version";
}
