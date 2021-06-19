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

package org.tomato.study.rpc.core.utils;

/**
 * @author Tomato
 * Created on 2021.06.13
 */
public final class ClassUtil {

    private ClassUtil() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    /**
     * search the ClassLoader in the following order:
     * 1. context class loader
     * 2. class type loader
     * 3. system class loader
     * @param clazz target class type
     * @return class loader
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader classLoader = null;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            // do nothing, continue
        }
        if (classLoader == null) {
            try {
                classLoader = clazz.getClassLoader();
            } catch (Throwable e) {
                // do nothing, continue
            }
        }
        if (classLoader == null) {
            try {
                classLoader = ClassLoader.getSystemClassLoader();
            } catch (Throwable e) {
                // do nothing
            }
        }
        return classLoader;
    }
}
