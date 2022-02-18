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
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RefreshInvokerTask;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.registry.zookeeper.ChildrenListener;
import org.tomato.study.rpc.registry.zookeeper.CuratorClient;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 监听微服务节点的孩子节点变化
 * @author Tomato
 * Created on 2021.07.10
 */
public class PathChildrenListener implements CuratorWatcher, ChildrenListener {

    private final MicroServiceSpace microService;
    private final NameServer nameServer;
    private final CuratorClient curatorClient;
    private volatile boolean unwatch;

    public PathChildrenListener(MicroServiceSpace microService,
                                NameServer nameServer,
                                CuratorClient curatorClient) {
        if (microService == null || nameServer == null || curatorClient == null) {
            throw new IllegalArgumentException(
                    "MicroServiceSpace、ZookeeperRegistry and CuratorClient must not be null");
        }
        this.microService = microService;
        this.nameServer = nameServer;
        this.curatorClient = curatorClient;
        this.unwatch = false;
    }

    @Override
    public void process(WatchedEvent watchedEvent) throws Exception {
        if (unwatch) {
            return;
        }

        // 获取监听的路径
        String path = watchedEvent.getPath();

        // 重新获取当前节点数据，并重新注册watcher
        List<String> children = curatorClient.getChildrenAndAddWatcher(path, this);

        // 更新zookeeper registry的数据
        childrenChanged(path, children);
    }

    @Override
    public void childrenChanged(String path, List<String> children) throws Exception {
        // 节点为空, 表明监听的父路径下已无实例节点
        if (CollectionUtils.isEmpty(children)) {
            nameServer.submitInvokerRefreshTask(
                    new RefreshInvokerTask(microService, Collections.emptySet()));
            return;
        }
        // RPC实例节点路径名包含了IP、端口等信息，将孩子节点路径转换为Metadata
        Set<MetaData> invokerInfo = children.stream()
                .map(child -> URI.create(URLDecoder.decode(
                        child, curatorClient.getZookeeperConfig().getCharset())))
                .map(MetaData::convert)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        // 将更新任务提交给NameServer
        nameServer.submitInvokerRefreshTask(new RefreshInvokerTask(microService, invokerInfo));
    }

    @Override
    public void unwatch() {
        unwatch = true;
    }
}
