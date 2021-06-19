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
