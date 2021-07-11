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

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.tomato.study.rpc.registry.zookeeper.ChildrenListener;
import org.tomato.study.rpc.zookeeper.CuratorClient;

/**
 * @author Tomato
 * Created on 2021.07.07
 */
@Builder
@AllArgsConstructor
public class PathChildrenWatcher implements CuratorWatcher {

    private final CuratorClient zkClient;

    private ChildrenListener childrenListener;

    @Override
    public void process(WatchedEvent watchedEvent) throws Exception {
        if (childrenListener == null) {
            return;
        }
        String path = watchedEvent.getPath();
        if (StringUtils.isBlank(path)) {
            return;
        }
        childrenListener.childrenChanged(path, zkClient.getChildren(path, this));
    }

    public void unwatch() {
        this.childrenListener = null;
    }
}
