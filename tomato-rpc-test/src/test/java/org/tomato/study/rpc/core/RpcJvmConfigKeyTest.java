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

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

/**
 * @author Tomato
 * Created on 2021.11.26
 */
public class RpcJvmConfigKeyTest {

    @Test
    @SuppressWarnings("all")
    public void testMultiKeyParse() {
        String key1 = "evdf";
        String value1 = "ewrfqwf";
        String key2 = "dsvgrega";
        String value2 = "afewtgarg";
        String key3 = "sdvf";
        String value3 ="efwegtwhbwt";
        String input = key1 + RpcJvmConfigKey.KEY_VALUE_DELIMITER + value1 + RpcJvmConfigKey.ENTRY_DELIMITER +
                key2 + RpcJvmConfigKey.KEY_VALUE_DELIMITER + value2 + RpcJvmConfigKey.ENTRY_DELIMITER +
                key3 + RpcJvmConfigKey.KEY_VALUE_DELIMITER + value3;
        Map<String, String> result = RpcJvmConfigKey.parseMultiKeyValue(input);
        Assert.assertTrue(Objects.equals(result.get(key1), value1));
        Assert.assertTrue(Objects.equals(result.get(key2), value2));
        Assert.assertTrue(Objects.equals(result.get(key3), value3));
    }
}
