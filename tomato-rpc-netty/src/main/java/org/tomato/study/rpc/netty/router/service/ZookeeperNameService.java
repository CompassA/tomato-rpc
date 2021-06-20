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

package org.tomato.study.rpc.netty.router.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.zookeeper.CuratorClient;
import org.tomato.study.rpc.zookeeper.WatcherHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
public class ZookeeperNameService extends WatcherHandler implements NameService {

    private static final String ZK_NAME_SPACE = "tomato";

    private boolean connected = false;

    private final ConcurrentMap<String, CopyOnWriteArrayList<MetaData>> uriMap = new ConcurrentHashMap<>();

    private CuratorClient curatorClient;

    @Override
    public synchronized void connect(URI nameServerURI, List<String> subscribedVIP) {
        if (connected) {
            return;
        }
        try {
            this.curatorClient = new CuratorClient(
                    String.format("%s:%d", nameServerURI.getHost(), nameServerURI.getPort()), ZK_NAME_SPACE);
            this.curatorClient.start();
            if (CollectionUtils.isNotEmpty(subscribedVIP)) {
                for (String vip : subscribedVIP) {
                    String path = "/" + vip;
                    List<String> children = this.curatorClient.getChildren(path);
                    CopyOnWriteArrayList<MetaData> metaDataList = new CopyOnWriteArrayList<>();
                    if (CollectionUtils.isNotEmpty(children)) {
                        for (String child : children) {
                            URI uri = URI.create(URLDecoder.decode(child, StandardCharsets.UTF_8.toString()));
                            metaDataList.add(
                                    MetaData.builder()
                                            .vip(vip)
                                            .uri(uri)
                                            .build()
                            );
                        }
                    }
                    this.uriMap.put(vip, metaDataList);
                    this.curatorClient.subscribe(path, this);
                }
            }
            this.connected = true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        try {
            this.curatorClient.close();
            this.connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerService(MetaData metaData) {
        try {
            curatorClient.create(metaData.toPath(), true, null);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Optional<URI> lookupService(String serviceVIP) throws Exception {
        CopyOnWriteArrayList<MetaData> list = uriMap.get(serviceVIP);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0).getUri());
    }

    @Override
    protected void handleNodeAdded(CuratorFramework client, TreeCacheEvent event) {
        //todo
    }

    @Override
    protected void handleNodeRemoved(CuratorFramework client, TreeCacheEvent event) {
        //todo
    }

    @Override
    protected void handleNodeUpdated(CuratorFramework client, TreeCacheEvent event) {
        //todo
    }
}
