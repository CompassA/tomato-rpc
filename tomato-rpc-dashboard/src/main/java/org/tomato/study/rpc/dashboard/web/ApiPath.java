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

package org.tomato.study.rpc.dashboard.web;

/**
 * 接口
 * @author Tomato
 * Created on 2022.08.07
 */
public class ApiPath {

    public static final String ROOT = "/tomato/api";

    public static class Stat {

        public static final String STAT_ROOT = ROOT + "/stat";

        public static final String APP_LIST = STAT_ROOT + "/apps";

        public static final String NODE_LIST = STAT_ROOT + "/nodes";
    }
}
