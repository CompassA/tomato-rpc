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

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.registry.BaseNameServer;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.registry.zookeeper.error.TomatoRegistryErrorEnum;
import org.tomato.study.rpc.registry.zookeeper.impl.ZookeeperRegistry;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 基于zookeeper实现的注册中心
 * @author Tomato
 * Created on 2021.06.19
 */
public class ZookeeperNameServer extends BaseNameServer {

    private static final String ZK_NAME_SPACE = "tomato";

    /**
     * 注册中心
     */
    private ZookeeperRegistry registry;

    public ZookeeperNameServer(NameServerConfig nameServerConfig) {
        super(nameServerConfig);
    }

    @Override
    public Optional<MicroServiceSpace> getMicroService(String serviceId) {
        return registry.getMicroService(serviceId);
    }

    @Override
    public void registerService(MetaData metaData) throws Exception {
        registry.register(metaData);
    }

    @Override
    public void unregisterService(MetaData metaData) throws Exception {
        registry.unregister(metaData);
    }

    @Override
    public void subscribe(MetaData rpcServerMetaData, MicroServiceSpace[] microServices, String stage) throws Exception {
        registry.subscribe(rpcServerMetaData, microServices, stage);
    }

    @Override
    public void unsubscribe(MicroServiceSpace[] microServices, String stage) throws Exception {
        registry.unsubscribe(microServices);
    }

    @Override
    public Optional<RpcInvoker> lookupInvoker(Invocation invocation) {
        return registry.lookup(invocation);
    }

    @Override
    public List<RpcInvoker> listInvokers(String microServiceId) {
        return registry.listInvokers(microServiceId);
    }

    @Override
    protected synchronized void doInit() throws TomatoRpcException {
        super.doInit();
        ZookeeperConfig zookeeperConfig = ZookeeperConfig.builder()
                .namespace(ZK_NAME_SPACE)
                .connString(getConnString())
                .build();
        this.registry = new ZookeeperRegistry(zookeeperConfig, this);
    }

    @Override
    protected synchronized void doStart() throws TomatoRpcException {
        super.doStart();
        registry.start();
    }

    @Override
    protected synchronized void doStop() throws TomatoRpcException {
        super.doStop();
        try {
            registry.close();
        } catch (IOException e) {
            throw new TomatoRpcException(
                    TomatoRegistryErrorEnum.RPC_REGISTRY_CLOSE_ERROR.create(), e);
        }
    }
}
