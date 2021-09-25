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

package org.tomato.study.rpc.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * Reflect Common Utils
 * @author Tomato
 * Created on 2021.07.18
 */
@Slf4j
public final class ReflectUtils {

    /**
     * set object field by reflect
     * @param instance object to set field
     * @param clazz object type
     * @param fieldName field name of the object to set
     * @param value value to set
     * @param <T> target object type
     */
    public static <T> void reflectSet(T instance,
                                  Class<T> clazz,
                                  String fieldName,
                                  Object value) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error(e.getMessage(), e);;
        }
    }

    /**
     * get object field by reflect
     * @param instance target object
     * @param clazz target object class
     * @param fieldName target field name
     * @param <T> instance type
     * @param <U> filed type
     * @return file value
     */
    @SuppressWarnings("unchecked")
    public static <T, U> U reflectGet(T instance,
                            Class<? extends T> clazz,
                            String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return (U) declaredField.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error(e.getMessage(), e);;
            return null;
        }
    }
}
