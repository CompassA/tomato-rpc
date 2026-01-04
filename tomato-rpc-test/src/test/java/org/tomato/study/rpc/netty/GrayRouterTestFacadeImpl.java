/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.data.MetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomato
 * Created on 2026.01.04
 */
@Slf4j
@Getter
@Setter
public class GrayRouterTestFacadeImpl implements GrayRouterTestFacade {

    private String id;

    private MetaData rpcServerMetaData;

    private GrayRouterTestFacade stub;

    @Override
    public List<String> pass(List<String> upstremList) {
        log.info("{} receive messge", rpcServerMetaData);
        List<String> metadataList = new ArrayList<>(upstremList);
        metadataList.add(MetaData.convert(rpcServerMetaData).get().toASCIIString());
        if (stub != null) {
            return stub.pass(metadataList);
        }
        return metadataList;
    }
}
