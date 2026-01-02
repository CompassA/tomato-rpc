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

package org.tomato.study.rpc.dashboard.dao;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.tomato.study.rpc.dashboard.assembler.RpcInvokerAssembler;
import org.tomato.study.rpc.dashboard.dao.data.RpcInvokerData;
import org.tomato.study.rpc.dashboard.exception.DashboardSystemException;
import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;
import org.tomato.study.rpc.registry.zookeeper.impl.ZookeeperRegistry;

import java.util.Collections;
import java.util.List;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@RequiredArgsConstructor
public class ZookeeperRegistryDAO implements NameServerDAO {

    public final CuratorFramework client;

    @Override
    public List<RpcInvokerData> listInvokers(String microServiceId, String stage) {
        String microServicePath = ZookeeperRegistry.buildServiceNodePath(microServiceId, stage);
        try {
            List<String> invokerUrls = client.getChildren().forPath(microServicePath);
            if (CollectionUtils.isEmpty(invokerUrls)) {
                return Collections.emptyList();
            }
            return invokerUrls.stream().map(RpcInvokerAssembler::toData).toList();
        } catch (Exception e) {
            throw new DashboardSystemException(e, ResponseCodeEnum.ZOOKEEPER_CLIENT_ERROR,
                String.format("getChildren for path %s failed", microServicePath));
        }
    }
}
