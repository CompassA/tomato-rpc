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
import org.tomato.study.rpc.dashboard.dao.data.RpcAppData;
import org.tomato.study.rpc.dashboard.dao.data.RpcAppProviderData;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@RequiredArgsConstructor
public class ZookeeperRegistryDAO implements NameServerDAO {

    public final CuratorFramework client;

    @Override
    public List<RpcAppData> listRpcData(int start, int nums) throws Exception {
        List<String> children = client.getChildren().forPath("/");
        if (CollectionUtils.isEmpty(children) || children.size() < start) {
            return Collections.emptyList();
        }
        int beginIndex = start-1;
        return children.subList(beginIndex, Math.min(beginIndex+nums, children.size())).stream()
                .map(app -> new RpcAppData(app))
                .collect(Collectors.toList());
    }

    @Override
    public List<RpcAppProviderData> listProviders(String appName, String stage) {
        return null;
    }
}
