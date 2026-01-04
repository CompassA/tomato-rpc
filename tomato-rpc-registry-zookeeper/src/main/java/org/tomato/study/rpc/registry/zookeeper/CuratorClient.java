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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.registry.zookeeper.data.ZookeeperConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.05.31
 */
public class CuratorClient implements Closeable {

    /**
     * curator client
     */
    @Getter
    private final CuratorFramework curatorClient;

    private final ConcurrentMap<String, TreeCache> listeners = new ConcurrentHashMap<>(0);

    @Getter
    private final ZookeeperConfig zookeeperConfig;

    /**
     * zookeeper namespace
     */
    @Getter
    private final String nameSpace;

    public CuratorClient(ZookeeperConfig zookeeperConfig) {
        this.zookeeperConfig = zookeeperConfig;
        this.nameSpace = zookeeperConfig.getNamespace();
        this.curatorClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperConfig.getConnString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                //15 seconds
                .connectionTimeoutMs(15 * 1000)
                //session timeout
                .sessionTimeoutMs(60 * 1000)
                //root path
                .namespace(nameSpace)
                .build();
    }

    public CuratorClient start() {
        this.curatorClient.start();
        return this;
    }

    public void createEphemeral(String path) throws Exception {
        this.create(path, true, null);
    }

    public void createEphemeral(String path, byte[] payload) throws Exception {
        this.create(path, true, payload);
    }

    public void create(String path, boolean ephemeral, byte[] payload) throws Exception {
        if (StringUtils.isBlank(path)) {
            return;
        }
        CreateMode createMode = ephemeral ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT;
        if (payload == null || payload.length == 0) {
            this.curatorClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(path);
        } else {
            this.curatorClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(path, payload);
        }
    }

    public Stat checkExists(String path) throws Exception {
        return this.curatorClient.checkExists().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return this.curatorClient.getChildren().forPath(path);
    }

    public List<String> getChildrenAndAddWatcher(String path, CuratorWatcher watcher) throws Exception {
        if (StringUtils.isBlank(path) || watcher == null) {
            return Collections.emptyList();
        }
        // create path if absent
        if (this.curatorClient.checkExists().forPath(path) == null) {
            try {
                this.curatorClient.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(path);
            } catch (KeeperException.NodeExistsException exception) {
                // catch node exist exception and logic continue
                Logger.DEFAULT.error(exception.getMessage(), exception);;
            }
        }

        return this.curatorClient.getChildren()
                .usingWatcher(watcher)
                .forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return this.curatorClient.getData().forPath(path);
    }

    public void update(String path, byte[] val) throws Exception {
        if (val == null || val.length < 1) {
            return;
        }
        this.curatorClient.setData().forPath(path, val);
    }

    public void delete(String path) throws Exception {
        if (StringUtils.isBlank(path)) {
            return;
        }
        this.curatorClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
    }

    public void subscribe(String path, TreeCacheListener listener) throws Exception {
        if (StringUtils.isBlank(path) || listener == null) {
            return;
        }
        TreeCache treeCache = new TreeCache(this.curatorClient, path);
        treeCache.start();
        treeCache.getListenable().addListener(listener);
        this.listeners.put(path, treeCache);
    }

    @Override
    public void close() throws IOException {
        if (this.curatorClient != null) {
            this.curatorClient.close();
        }
    }
}
