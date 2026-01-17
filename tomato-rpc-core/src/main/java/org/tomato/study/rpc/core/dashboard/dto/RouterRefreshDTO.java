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

package org.tomato.study.rpc.core.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Tomato
 * Created on 2026.01.04
 */
@Getter
@Setter
public class RouterRefreshDTO {

    /**
     * 全局唯一递增ID
     */
    private Long opsGlobalId;

    /**
     * 路由规则目标微服务ID
     */
    private String routerMicroServiceId;

    /**
     * 待更新的规则
     */
    private List<RpcRouterDTO> rpcRouterDTOList;
}
