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

package org.tomato.study.rpc.registry.zookeeper;

import org.tomato.study.rpc.core.base.BaseNameService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.router.RpcInvoker;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.registry.zookeeper.error.TomatoRegistryErrorEnum;
import org.tomato.study.rpc.registry.zookeeper.impl.ZookeeperRegistry;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class ZookeeperNameService extends BaseNameService {

    private static final String ZK_NAME_SPACE = "tomato";

    private ZookeeperRegistry registry;

    public ZookeeperNameService(NameServerConfig nameServerConfig) {
        super(nameServerConfig);
    }

    @Override
    public void registerService(MetaData metaData) throws Exception {
        registry.register(metaData);
    }

    @Override
    public void subscribe(Collection<String> vipList, String stage) throws Exception {
        registry.subscribe(vipList, stage);
    }

    @Override
    public Optional<RpcInvoker> lookupInvoker(String serviceVIP, String version) {
        return registry.lookup(serviceVIP, version);
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        registry = new ZookeeperRegistry(
                ZookeeperConfig.builder()
                        .namespace(ZK_NAME_SPACE)
                        .connString(getConnString())
                        .charset(getCharset())
                        .build()
        );
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        registry.start();
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        try {
            registry.close();
        } catch (IOException e) {
            throw new TomatoRpcException(
                    TomatoRegistryErrorEnum.RPC_REGISTRY_CLOSE_ERROR.create(), e);
        }
    }
}
