package org.tomato.study.rpc.zookeeper;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Tomato
 * Created on 2021.05.31
 */
public class CuratorClient implements Closeable {

    private final CuratorFramework curatorClient;

    private final ConcurrentMap<String, TreeCache> listeners = new ConcurrentHashMap<>(0);

    @Getter
    private final String nameSpace;

    public CuratorClient(String connStr, String nameSpace) {
        this.nameSpace = nameSpace;
        this.curatorClient = CuratorFrameworkFactory.builder()
                .connectString(connStr)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                //15 seconds
                .connectionTimeoutMs(15 * 1000)
                //session timeout
                .sessionTimeoutMs(60 * 1000)
                //root path
                .namespace(nameSpace)
                .build();
    }

    public void start() {
        this.curatorClient.start();
    }

    public void create(String path, boolean ephemeral, byte[] payload) throws Exception {
        if (StringUtils.isBlank(path)) {
            return;
        }
        CreateMode createMode = ephemeral ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT;
        if (payload == null || payload.length == 0) {
            curatorClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(path);
        } else {
            curatorClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(path, payload);
        }
    }

    public Stat checkExists(String path) throws Exception {
        return curatorClient.checkExists().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return curatorClient.getChildren().forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return curatorClient.getData().forPath(path);
    }

    public void update(String path, byte[] val) throws Exception {
        if (val == null || val.length < 1) {
            return;
        }
        curatorClient.setData().forPath(path, val);
    }

    public void delete(String path) throws Exception {
        if (StringUtils.isBlank(path)) {
            return;
        }
        curatorClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
    }

    public void subscribe(String path, TreeCacheListener listener) throws Exception {
        if (StringUtils.isBlank(path) || listener == null) {
            return;
        }
        TreeCache treeCache = new TreeCache(curatorClient, path);
        treeCache.start();
        treeCache.getListenable().addListener(listener);
        this.listeners.put(path, treeCache);
    }

    @Override
    public void close() throws IOException {
        if (curatorClient != null) {
            curatorClient.close();
        }
        for (TreeCache value : listeners.values()) {
            value.close();
        }
        listeners.clear();
    }
}
