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

package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.tomato.study.rpc.core.NameServer;

/**
 * necessary data for creating stub
 * @author Tomato
 * Created on 2021.07.13
 */
@Getter
@AllArgsConstructor
public class StubConfig<T> {

    /**
     * name service
     */
    private final NameServer nameServer;

    /**
     * interface of service provider
     */
    private final Class<T> serviceInterface;

    /**
     * service vip
     */
    private final String serviceVIP;

    /**
     * service group
     */
    private final String group;
}
