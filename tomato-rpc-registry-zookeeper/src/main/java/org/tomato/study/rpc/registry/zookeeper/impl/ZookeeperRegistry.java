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

package org.tomato.study.rpc.registry.zookeeper.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.core.dashboard.data.RpcRouterData;
import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.error.TomatoRpcErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;
import org.tomato.study.rpc.core.registry.NameServer;
import org.tomato.study.rpc.core.router.BaseMicroServiceSpace;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.registry.zookeeper.ChildrenListener;
import org.tomato.study.rpc.registry.zookeeper.CuratorClient;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;
import org.tomato.study.rpc.registry.zookeeper.utils.ZookeeperAssembler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于zookeeper实现的注册中心
 * @author Tomato
 * Created on 2021.07.07
 */
public class ZookeeperRegistry {

    /**
     * curator client
     */
    private final CuratorClient curatorClient;

    /**
     * name server
     */
    private final NameServer owner;

    /**
     * 服务唯一标识 -> MicroService
     */
    private final Map<String, MicroServiceSpace> microServiceMap = new HashMap<>(0);

    /**
     * MicroService -> children listener
     */
    private final ConcurrentMap<MicroServiceSpace, ChildrenListener> listenerMap = new ConcurrentHashMap<>(0);

    public ZookeeperRegistry(ZookeeperConfig config, NameServer owner) {
        this.curatorClient = new CuratorClient(config);
        this.owner = owner;
    }

    public void start() {
        curatorClient.start();
    }

    /**
     * 将服务ip端口暴露至zookeeper
     * @param metaData 服务ip、端口等元数据
     * @throws Exception exceptions during register
     */
    public void register(MetaData metaData) throws Exception {
        Optional<URI> uriOpt = MetaData.convert(metaData);
        if (uriOpt.isEmpty()) {
            return;
        }
        // 路径：/namespace/micro-service-id/stage/providers/ip+port....
        String zNodePath = ZookeeperAssembler.buildServiceNodePath(
            metaData.getMicroServiceId(),
            metaData.getStage(),
            uriOpt.get());
        curatorClient.createEphemeral(zNodePath);
    }

    /**
     * 将服务元数据从zookeeper摘除
     * @param metaData provider metadata
     * @throws Exception exception during unregister
     */
    public void unregister(MetaData metaData) throws Exception {
        Optional<URI> uriOpt = MetaData.convert(metaData);
        if (uriOpt.isEmpty()) {
            return;
        }
        String zNodePath = ZookeeperAssembler.buildServiceNodePath(
            metaData.getMicroServiceId(),
            metaData.getStage(),
            uriOpt.get());
        curatorClient.delete(zNodePath);
    }

    /**
     * 订阅其他RPC服务
     * @param microServices 要订阅的RPC服务列表
     * @param stage 当前服务的环境
     * @throws Exception exception during subscribe
     */
    public void subscribe(MetaData rpcServerMetaData, MicroServiceSpace[] microServices, String stage) throws Exception {
        if (microServices == null || microServices.length < 1) {
            return;
        }
        // 订阅目标节点
        for (MicroServiceSpace microService : microServices) {
            String microServiceId = microService.getMicroServiceId();
            if (StringUtils.isBlank(microServiceId)) {
                throw new TomatoRpcRuntimeException(TomatoRpcErrorEnum.RPC_CONFIG_INITIALIZING_ERROR, "microServiceId is blank");
            }
            // 保存要订阅的微服务对象
            microServiceMap.putIfAbsent(microServiceId, microService);

            // 根据固定的 /micro-service-id/stage/PROVIDER_DICTIONARY规则，计算出被订阅服务的zookeeper路径
            String targetPath = ZookeeperAssembler.buildServiceNodeParent(microServiceId, stage);

            // 创建WATCHER, 监听服务节点的子节点变化，保证服务实例的更新与删除能同步到订阅方的内存中
            ChildrenListener listener = listenerMap.computeIfAbsent(microService,
                service -> new PathChildrenListener(microService, owner, curatorClient));

            // 获取目标服务的所有RPC实例节点, 并注册WATCHER
            List<String> invokerUrlList = curatorClient.getChildrenAndAddWatcher(targetPath, listener);
            if (CollectionUtils.isEmpty(invokerUrlList)) {
                continue;
            }

            // 将RPC实例节点路径解码并转成Metadata形式
            final Set<MetaData> metadata = new HashSet<>(invokerUrlList.size());
            for (String invokerUrl : invokerUrlList) {
                ZookeeperAssembler.convertToModel(invokerUrl)
                    .filter(meta -> {
                        if (meta.isValid()) {
                            return true;
                        }
                        Logger.DEFAULT.warn("invalid invoker url: {}", invokerUrl);
                        return false;
                    }).ifPresent(metadata::add);
            }

            // 更新微服务的数据
            microService.refresh(metadata);
        }

        // 读取路由规则
        for (MicroServiceSpace microService : microServices) {
            String microServiceId = microService.getMicroServiceId();
            String path = ZookeeperAssembler.buildServiceRouterPath(rpcServerMetaData.getMicroServiceId(), rpcServerMetaData.getStage(), microServiceId);
            if (curatorClient.checkExists(path) != null) {
                byte[] raw = curatorClient.getData(path);
                RpcRouterData routerData = ZookeeperAssembler.toRouterData(raw);
                microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, routerData.getExpr());
            } else {
                microService.refreshRouter(BaseMicroServiceSpace.INITIAL_ROUTER_OPS_ID, Collections.emptyList());
            }
        }
    }

    /**
     * 取消订阅RPC服务
     * @param microServices 取消订阅的RPC服务列表
     */
    public void unsubscribe(MicroServiceSpace[] microServices) throws TomatoRpcException {
        if (microServices == null || microServices.length < 1) {
            return;
        }
        for (MicroServiceSpace microService : microServices) {
            String microServiceId = microService.getMicroServiceId();
            if (StringUtils.isBlank(microServiceId)) {
                continue;
            }
            // 移除微服务对象
            MicroServiceSpace provider = microServiceMap.remove(microServiceId);
            if (provider == null) {
                continue;
            }
            provider.close();

            // 移除对应的Watcher
            ChildrenListener listener = listenerMap.remove(provider);
            if (listener != null) {
                listener.unwatch();
            }
        }
    }

    /**
     * 寻找服务端Invoker
     * @param invocation 客户端参数
     * @return 服务端Invoker
     */
    public Optional<RpcInvoker> lookup(Invocation invocation) {
        String microServiceId = invocation.getMicroServiceId();
        if (StringUtils.isBlank(microServiceId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(microServiceMap.get(microServiceId))
                .flatMap(provider -> provider.lookUp(invocation));
    }

    public List<RpcInvoker> listInvokers(String microServiceId) {
        MicroServiceSpace microServiceSpace = microServiceMap.get(microServiceId);
        if (microServiceSpace == null) {
            return Collections.emptyList();
        }
        return microServiceSpace.getAllInvokers();
    }

    public synchronized void close() throws IOException {
        curatorClient.close();
    }

    public Optional<MicroServiceSpace> getMicroService(String serviceId) {
        return Optional.ofNullable(microServiceMap.get(serviceId));
    }
}
