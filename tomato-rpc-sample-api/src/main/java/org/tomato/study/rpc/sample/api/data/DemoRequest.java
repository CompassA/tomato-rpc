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

/**
 * @author Tomato
 * Created on 2021.06.20
 */
public class DemoRequest {

    private String data;
    private final TestDTO testDTO = new TestDTO();

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

    public TestDTO getTestDTO() {
        return testDTO;
    }

    @Override
    public String toString() {
        return "DemoRequest{" +
            "data='" + data + '\'' +
            ", testDTO=" + testDTO +
            '}';
    }
}
