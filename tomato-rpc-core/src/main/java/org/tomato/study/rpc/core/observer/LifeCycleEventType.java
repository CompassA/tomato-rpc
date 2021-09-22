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

package org.tomato.study.rpc.core.observer;

import lombok.Getter;

/**
 * event types
 * @author Tomato
 * Created on 2021.09.23
 */
@Getter
public enum LifeCycleEventType {
    /**
     * linked with {@link LifeCycle}
     */
    BEFORE_INIT(0),
    AFTER_INIT(1),
    BEFORE_START(2),
    AFTER_START(3),
    BEFORE_STOP(4),
    AFTER_STOP(5),
    ;

    /**
     * unique id
     */
    private final int id;

    LifeCycleEventType(int id) {
        this.id = id;
    }

}
