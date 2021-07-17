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

import com.google.common.collect.Lists;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.zookeeper.data.Stat;
import org.junit.Assert;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.06.02
 */
public class CuratorClientTest {

    //@Test
    public void localTest() throws Exception {
        String nameSpace = "test";
        String path = "/add";
        CuratorClient curatorClient = new CuratorClient("127.0.0.1:2181", nameSpace);
        curatorClient.start();
        curatorClient.subscribe("/", new TestTreeCacheListener());

        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        curatorClient.create(path, false, data);
        Assert.assertArrayEquals(data, curatorClient.getData(path));

        byte[] newData = "new data".getBytes(StandardCharsets.UTF_8);
        curatorClient.update(path, newData);
        Assert.assertArrayEquals(newData, curatorClient.getData(path));

        List<String> children = Lists.newArrayList("1", "2", "3", "4", "5");
        children.forEach(child -> {
            try {
                curatorClient.create(path + "/" + child, true, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Assert.assertEquals(children, curatorClient.getChildren(path));

        curatorClient.delete(path);
        Assert.assertNull(curatorClient.checkExists(path));

        curatorClient.close();
    }

    private static class TestTreeCacheListener implements TreeCacheListener {
        @Override
        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            ChildData data = event.getData();
            byte[] nodeData = Optional.ofNullable(data)
                    .map(ChildData::getData)
                    .orElse("null".getBytes(StandardCharsets.UTF_8));
            Stat dataStat = Optional.ofNullable(data)
                    .map(ChildData::getStat)
                    .orElse(null);
            String path = Optional.ofNullable(data)
                    .map(ChildData::getPath)
                    .orElse("null");
            String fmt = "event type: %s\ndata stat: %sdata path: %s\nnode data: %s\n\n";
            switch (event.getType()) {
                case NODE_ADDED:
                    System.out.printf(fmt, "NODE_ADDED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case NODE_REMOVED:
                    System.out.printf(fmt, "NODE_REMOVED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case NODE_UPDATED:
                    System.out.printf(fmt, "NODE_UPDATED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case CONNECTION_LOST:
                    System.out.printf(fmt, "CONNECTION_LOST", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case CONNECTION_RECONNECTED:
                    System.out.printf(fmt, "CONNECTION_RECONNECTED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case CONNECTION_SUSPENDED:
                    System.out.printf(fmt, "CONNECTION_SUSPENDED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                case INITIALIZED:
                    System.out.printf(fmt, "INITIALIZED", dataStat, path, new String(nodeData, StandardCharsets.UTF_8));
                    break;
                default:
                    break;
            }
        }
    }

}
