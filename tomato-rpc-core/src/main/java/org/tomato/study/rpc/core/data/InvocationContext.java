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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RPC上下文
 * @author Tomato
 * Created on 2026.01.04
 */
public class InvocationContext {

    public static final ThreadLocal<Map<String, String>> CONTEXT = new ThreadLocal<>();

    public static Map<String, String> initContext() {
        Map<String, String> context = new HashMap<>();
        context.put(ExtensionHeader.TRACE_ID.getKeyName(), UUID.randomUUID().toString().replaceAll("-", ""));
        set(context);
        return context;
    }

    public static void set(Map<String, String> context) {
        CONTEXT.set(context);
    }

    public static void put(String key, String value) {
        get().put(key, value);
    }

    public static String get(String key) {
        Map<String, String> contextMap = get();
        if (contextMap == null) {
            return null;
        }
        return contextMap.get(key);
    }

    public static Map<String, String> get() {
        return CONTEXT.get();
    }

    public static void remove() {
        CONTEXT.remove();
    }
}
