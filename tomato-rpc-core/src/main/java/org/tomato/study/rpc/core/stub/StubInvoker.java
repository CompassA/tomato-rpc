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

package org.tomato.study.rpc.core.stub;

import java.lang.reflect.Method;

/**
 * 代理对象具体逻辑
 * @author Tomato
 * Created on 2021.11.27
 */
public interface StubInvoker {

    /**
     * invoke method
     * @param proxy proxy instance
     * @param method proxy method
     * @param args method args
     * @return invoke result
     */
    Object invoke(Object proxy, Method method, Object[] args);

    /**
     * target micro-service id
     * @return id
     */
    String getMicroServiceId();

    /**
     * rpc group
     * @return rpc group
     */
    String getGroup();

    /**
     * get proxy target interface
     * @return interface
     */
    Class<?> getServiceInterface();
}
