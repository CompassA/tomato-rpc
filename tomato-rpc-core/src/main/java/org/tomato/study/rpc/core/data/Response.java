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

/**
 * invocation response
 * @author Tomato
 * Created on 2021.07.17
 */
public interface Response {

    /**
     * response status code
     * @return status code
     */
    int getCode();

    /**
     * get method call result
     * @return result data
     */
    Object getData();

    /**
     * get error message
     * @return error message
     */
    String getMessage();
}
