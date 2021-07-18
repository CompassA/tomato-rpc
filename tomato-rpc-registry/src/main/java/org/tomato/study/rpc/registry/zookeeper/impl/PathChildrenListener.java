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
import org.tomato.study.rpc.registry.zookeeper.ChildrenListener;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tomato
 * Created on 2021.07.10
 */
@AllArgsConstructor
public class PathChildrenListener implements ChildrenListener {

    private final ZookeeperRegistry registry;

    @Override
    public void childrenChanged(String path, List<String> children) throws IOException {
        if (children == null) {
            this.registry.notify(path, Collections.emptyList());
            return;
        }
        this.registry.notify(
                path,
                children.stream()
                        .map(child -> URI.create(
                                URLDecoder.decode(child, registry.getZNodePathCharset())))
                        .collect(Collectors.toList())
        );
    }
}
