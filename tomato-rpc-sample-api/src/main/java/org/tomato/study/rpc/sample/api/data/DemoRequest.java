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

package org.tomato.study.rpc.sample.api.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tomato
 * Created on 2021.06.20
 */
public class DemoRequest {

    private String data;

    private final List<Map<String, Long>> testMap = new ArrayList<>(List.of(
            new HashMap<>(Map.of("a", 1L, "b", 2L, "c", 3L)),
            new HashMap<>(Map.of("c", 4L, "d", 5L, "e", 6L)),
            new HashMap<>(Map.of("e", 7L, "f", 8L, "g", 9L))
    ));

    private final List<Integer> testList = new ArrayList<>(List.of(1, 2, 3, 4));

    public DemoRequest() {
    }

    public DemoRequest(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<Map<String, Long>> getTestMap() {
        return testMap;
    }

    public List<Integer> getTestList() {
        return testList;
    }

    @Override
    public String toString() {
        return "DemoRequest{" +
                "data='" + data + '\'' +
                ", testMap=" + testMap +
                ", testList=" + testList +
                '}';
    }
}
