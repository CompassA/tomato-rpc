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

/**
 * component common life cycle
 * @author Tomato
 * Created on 2021.09.23
 */
public interface LifeCycle {

    /**
     * states
     */
    int CREATED = 0;
    int INIT = 1;
    int START = 2;
    int STOP = 3;

    /**
     * initialize method
     */
    void init();

    /**
     * start method
     */
    void start();

    /**
     * destroy method
     */
    void destroy();
}
