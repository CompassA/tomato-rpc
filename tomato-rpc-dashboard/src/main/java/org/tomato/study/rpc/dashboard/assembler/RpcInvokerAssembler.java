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

package org.tomato.study.rpc.dashboard.assembler;

import org.tomato.study.rpc.core.dashboard.data.RpcInvokerData;
import org.tomato.study.rpc.core.dashboard.data.RpcRouterData;
import org.tomato.study.rpc.core.dashboard.dto.RpcRouterDTO;
import org.tomato.study.rpc.core.dashboard.model.RpcInvokerModel;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.dashboard.exception.DashboardParamException;
import org.tomato.study.rpc.dashboard.web.view.RpcInvokersVO;
import org.tomato.study.rpc.registry.zookeeper.utils.ZookeeperAssembler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author Tomato
 * Created on 2026.01.02
 */
public class RpcInvokerAssembler {

    public static RpcInvokersVO toVO(String microServiceId, List<RpcInvokerModel> models) {
        List<RpcInvokersVO.RpcInvokerVO> invokers = new ArrayList<>();
        for (RpcInvokerModel model : models) {
            Optional<URI> convert = MetaData.convert(model.getMeta());
            if (convert.isEmpty()) {
                continue;
            }
            invokers.add(new RpcInvokersVO.RpcInvokerVO(convert.get().toASCIIString()));
        }
        return new RpcInvokersVO(microServiceId, invokers);
    }

    public static RpcInvokerData toData(String url) {
        Optional<MetaData> opt = ZookeeperAssembler.convertToModel(url);
        if (opt.isEmpty()) {
            throw new DashboardParamException("illegal invoker url: " + url);
        }

        MetaData metaData = opt.get();

        RpcInvokerData rpcInvokerData = new RpcInvokerData();
        rpcInvokerData.setMicroServiceId(metaData.getMicroServiceId());
        rpcInvokerData.setNodeProperties(metaData);
        return rpcInvokerData;
    }



    public static RpcRouterData toRouterData(List<RpcRouterDTO> dto) {
        dto.sort(Comparator.comparing(RpcRouterDTO::getPriority));

        List<String> expr = new ArrayList<>(dto.size());
        for (RpcRouterDTO rpcRouterDTO : dto) {
            expr.add(rpcRouterDTO.getExpr());
        }

        RpcRouterData rpcRouterData = new RpcRouterData();
        rpcRouterData.setExpr(expr);
        return rpcRouterData;
    }

    public static List<RpcRouterDTO> toRouterDTO(RpcRouterData data) {
        List<RpcRouterDTO> dtoList = new ArrayList<>();
        List<String> expr = data.getExpr();
        for (int i = 0; i < expr.size(); i++) {
            RpcRouterDTO dto = new RpcRouterDTO();
            dto.setPriority(i+1);
            dto.setExpr(expr.get(i));

            dtoList.add(dto);
        }
        return dtoList;
    }
}
