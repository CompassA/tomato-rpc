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

package org.tomato.study.rpc.core.data;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2021.07.10
 */
public class MetaDataTest {

    @Test
    public void convertTest() {
        MetaData metaData = mockMetaData();
        Optional<URI> uri = MetaData.convert(metaData);
        Assert.assertTrue(uri.isPresent());
        Optional<MetaData> metadataOpt = MetaData.convert(uri.get());

        Assert.assertTrue(metadataOpt.isPresent());
        Assert.assertEquals(metaData, metadataOpt.get());
    }

    @Test
    public void hashCodeTest() {
        List<MetaData> metaDataList = new ArrayList<>(0);
        HashMap<MetaData, Integer> map = new HashMap<>();
        int limit = 100;
        for (int i = 0; i < limit; ++i) {
            metaDataList.add(mockMetaData());
            map.put(mockMetaData(), i);
        }
        HashSet<MetaData> set = new HashSet<>(metaDataList);
        Assert.assertEquals(1, set.size());
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(limit - 1, (int) map.get(mockMetaData()));
    }

    @Test
    public void nullValueTest() {
        MetaData nullDataA = mockNull();
        Assert.assertFalse(MetaData.convert(nullDataA).isPresent());
    }

    private MetaData mockMetaData() {
        return MetaData.builder()
                .protocol("tomato")
                .host("127.0.0.1")
                .port(1234)
                .vip("org.study.test")
                .stage("dev")
                .group("test")
                .build();
    }

    private MetaData mockNull() {
        return  MetaData.builder()
                .vip("mock")
                .build();
    }
}
