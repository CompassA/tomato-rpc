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

package org.tomato.study.rpc.config.data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 通过spring配置tomato-rpc
 * @author Tomato
 * Created on 2021.11.18
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tomato-rpc")
public class TomatoRpcProperties {

    /**
     * 微服务唯一标识
     */
    private String microServiceId;

    /**
     * 订阅的其他微服务的唯一标识
     */
    private List<String> subscribedServices;

    /**
     * 注册中心的ip
     */
    private String nameServiceUri;

    /**
     * rpc服务端口
     */
    private Integer port;

    /**
     * rpc服务端业务线程池数量
     */
    private Integer businessThread;

    /**
     * rpc业务环境
     */
    private String stage;

    /**
     * rpc分组
     */
    private String group;

    /**
     * 服务端空闲检测频率, 单位ms
     */
    private Long serverIdleCheckMs;

    /**
     * 客户端心跳发送频率, 单位ms
     */
    private Long clientKeepAliveMs;

    /**
     * 客户端发送数据是否开启GZIP压缩
     */
    private boolean useGzip;
}
