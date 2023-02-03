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

package org.tomato.study.rpc.core.data;

import java.util.Map;
import java.util.Optional;

/**
 * the necessary data for a remote procedure call
 * @author Tomato
 * Created on 2021.07.07
 */
public interface Invocation {

    /**
     * get rpc service id in the service name registry
     * @return service id
     */
    String getMicroServiceId();

    /**
     * get rpc api class full name
     * @return class full name
     */
    String getInterfaceName();

    /**
     * get rpc api method name
     * @return method name
     */
    String getMethodName();

    /**
     * get method parameter types
     * @return parameter type array
     */
    String[] getArgsTypes();

    /**
     * get parameters of the method
     * @return parameters
     */
    Object[] getArgs();

    /**
     * get rpc api method return type
     * @return api method return type
     */
    String getReturnType();

    /**
     * 方法级别的api id
     * @return api id
     */
    default String getApiId() {
        return getMicroServiceId() + "$" + getInterfaceName() + "$" + getMethodName();
    }

    /**
     * 获取调用的一些参数
     * @return 参数map
     */
    Map<String, String> fetchContextMap();

    /**
     * put context parameter
     * @param key parameter key
     * @param value parameter value
     */
    void putContextParameter(String key, String value);

    /**
     * get parameter value
     * @param key parameter key
     * @return parameter value
     */
    Optional<String> fetchContextParameter(String key);

    /**
     * 构造一个没有ParameterContext的对象
     * @return InvocationWithoutContext
     */
    Invocation cloneInvocationWithoutContext();
}
