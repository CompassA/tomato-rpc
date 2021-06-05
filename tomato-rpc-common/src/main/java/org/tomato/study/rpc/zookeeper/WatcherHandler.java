package org.tomato.study.rpc.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * @author Tomato
 * Created on 2021.06.06
 */
public abstract class WatcherHandler implements TreeCacheListener {

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        switch (event.getType()) {
            case NODE_ADDED:
                handleNodeAdded(client, event);
                break;
            case NODE_REMOVED:
                handleNodeRemoved(client, event);
                break;
            case NODE_UPDATED:
                handleNodeUpdated(client, event);
                break;
            case CONNECTION_LOST:
                handleConnectionLost(client, event);
                break;
            case CONNECTION_RECONNECTED:
                handleConnectionReconnected(client, event);
                break;
            case CONNECTION_SUSPENDED:
                handleConnectionSuspended(client, event);
                break;
            case INITIALIZED:
                handlerInitialized(client, event);
                break;
            default:
        }
    }

    protected abstract void handleNodeAdded(CuratorFramework client, TreeCacheEvent event);

    protected abstract void handleNodeRemoved(CuratorFramework client, TreeCacheEvent event);

    protected abstract void handleNodeUpdated(CuratorFramework client, TreeCacheEvent event);

    protected void handleConnectionLost(CuratorFramework client, TreeCacheEvent event) {
    }

    protected void handleConnectionReconnected(CuratorFramework client, TreeCacheEvent event) {
    }

    protected void handleConnectionSuspended(CuratorFramework client, TreeCacheEvent event) {
    }

    protected void handlerInitialized(CuratorFramework client, TreeCacheEvent event) {
    }
}
