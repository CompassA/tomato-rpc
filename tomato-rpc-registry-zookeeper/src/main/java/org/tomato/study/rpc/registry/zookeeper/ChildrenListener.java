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

import org.apache.curator.framework.api.CuratorWatcher;

import java.io.IOException;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.07.10
 */
public interface ChildrenListener extends CuratorWatcher {

    /**
     * 当目标路径发送变动时，回调当前函数
     * @param path 被订阅的目标路径
     * @param children 目标路径更新后的子节点
     * @throws IOException exception during children changed callback
     */
    void childrenChanged(String path, List<String> children) throws Exception;

    /**
     * 取消订阅
     */
    void unwatch();
}
