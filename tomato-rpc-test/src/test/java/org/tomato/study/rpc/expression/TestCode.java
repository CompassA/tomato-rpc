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

package org.tomato.study.rpc.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomato
 * Created on 2022.01.02
 */
public class TestCode {

    public static List<String> CODES = new ArrayList<>(0);
    static {
        CODES.add("group  ==  \"dev\" && host == \"101.1.1.1\" -> group == \"perf\"");
        CODES.add("id   % 10 == 0 -> group == 101");
        CODES.add("(id   - 100 <= 50) && (id  * 100 >= 30) || (group == \"grey\") -> group == \"grey\" ");
        CODES.add("id + 10 > 1 && id / 10 < 2 -> (group == \"grey\")");
        CODES.add("a = v");
    }
}
