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

package org.tomato.study.rpc.config.controller;

/**
 * @author Tomato
 * Created on 2022.02.01
 */
public class ApiUrl {

    private static final String ROOT = "/tomato";

    public static class Status {
        private static final String STATUS_ROOT = "/status";

        /**
         * 获取某个服务的invoker信息
         */
        public static final String INVOKER_STATUS = ROOT + STATUS_ROOT + "/invoker";

        /**
         * rpc是否准备就绪
         */
        public static final String READY = ROOT + STATUS_ROOT + "/ready";
    }
}
