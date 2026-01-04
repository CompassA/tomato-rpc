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

package org.tomato.study.rpc.common.utils;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomato
 * Created on 2021.06.12
 */
public class UtilTest {

    @Test
    public void networkUtilTest() {
        String localHost = NetworkUtil.getLocalHost();
        Assert.assertTrue(StringUtils.isNotBlank(localHost) && !"127.0.0.1".equals(localHost));
    }

    @Test
    public void reflectUtilTest() {
        int intVal = 1;
        String strVal = "test_str";
        ReflectTestObject testObject = new ReflectTestObject(intVal, strVal);
        Assert.assertEquals(ReflectUtils.reflectGet(testObject, ReflectTestObject.class, "intField"), (Integer) intVal);

        strVal = "new_str";
        ReflectUtils.reflectSet(testObject,  ReflectTestObject.class, "stringField", strVal);
        Assert.assertEquals(ReflectUtils.reflectGet(testObject, ReflectTestObject.class, "stringField"), strVal);
    }

    @AllArgsConstructor
    public static class ReflectTestObject {
        private int intField;
        private String stringField;
    }
}
