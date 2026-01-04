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

package org.tomato.study.rpc.common.monitor;

/**
 * @author Tomato
 * Created on 2022.02.01
 */
public class ApiUrl {

    /**
     * 获取订阅的某个服务的invoker信息
     */
    public static final String INVOKER_STATUS = "/api/tomato/invoker/local/status";

    /**
     * rpc是否准备就绪
     */
    public static final String INVOKER_READY = "/api/tomato/invoker/local/ready";

    /**
     * 获取当前机器的路由规则
     */
    public static final String ROUTER_LIST = "/api/tomato/router/local/list";

    /**
     * 刷新Router数据
     */
    public static final String ROUTER_REFRESH = "/api/tomato/router/local/refresh";

}
