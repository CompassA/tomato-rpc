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

package org.tomato.study.rpc.core.circuit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.utils.ClassUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Tomato
 * Created on 2022.01.30
 */
@Slf4j
@AllArgsConstructor
public class CircuitBreakerHandler implements InvocationHandler {

    private Object object;
    private CircuitBreaker breaker;
    private Object failBackResult;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (!breaker.allow()) {
                return failBackResult;
            }
            Object res = method.invoke(object, args);
            breaker.addSuccess();
            return res;
        } catch (Throwable e) {
            breaker.addFailure();
            if (failBackResult != null) {
                return failBackResult;
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T object, CircuitBreaker breaker, Object failBackResult) {
        return (T) Proxy.newProxyInstance(
                ClassUtil.getClassLoader(object.getClass()),
                new Class[] {object.getClass()},
                new CircuitBreakerHandler(object, breaker, failBackResult));
    }
}
