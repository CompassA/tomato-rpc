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

package org.tomato.study.rpc.test;

import java.util.List;

/**
 * @author Tomato
 * Created on 2026.01.05
 */
public record TimeoutTestImpl(Long sleepMs,
                              TestService testService) implements TimeoutTest {

    @Override
    public Integer sum(List<Integer> numbers) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return testService.sum(numbers);
    }
}
