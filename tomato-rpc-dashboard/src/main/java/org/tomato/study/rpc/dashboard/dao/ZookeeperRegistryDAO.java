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

package org.tomato.study.rpc.dashboard.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.tomato.study.rpc.common.monitor.ApiUrl;
import org.tomato.study.rpc.core.dashboard.data.RpcInvokerData;
import org.tomato.study.rpc.core.dashboard.data.RpcRouterData;
import org.tomato.study.rpc.core.dashboard.dto.RouterRefreshDTO;
import org.tomato.study.rpc.core.dashboard.dto.RpcRouterDTO;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.dashboard.assembler.RpcInvokerAssembler;
import org.tomato.study.rpc.dashboard.exception.DashboardRuntimeException;
import org.tomato.study.rpc.dashboard.exception.DashboardSystemException;
import org.tomato.study.rpc.dashboard.web.view.ResponseCodeEnum;
import org.tomato.study.rpc.registry.zookeeper.utils.ZookeeperAssembler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Tomato
 * Created on 2022.08.07
 */
@Slf4j
public record ZookeeperRegistryDAO(CuratorFramework client,
                                   CloseableHttpClient httpClient,
                                   ThreadPoolExecutor refreshRouterExecutor,
                                   ObjectMapper mapper) implements NameServerDAO {

    @Override
    public List<RpcInvokerData> listInvokers(String microServiceId, String stage) {
        String microServicePath = ZookeeperAssembler.buildServiceNodeParent(microServiceId, stage);
        try {
            List<String> invokerUrls = client.getChildren().forPath(microServicePath);
            if (CollectionUtils.isEmpty(invokerUrls)) {
                return Collections.emptyList();
            }
            return invokerUrls.stream().map(RpcInvokerAssembler::toData).toList();
        } catch (Exception e) {
            throw new DashboardSystemException(e, ResponseCodeEnum.ZOOKEEPER_CLIENT_ERROR,
                String.format("getChildren for path %s failed", microServicePath));
        }
    }

    @Override
    public RpcRouterData listRouters(String microServiceId, String stage, String routerMicroServiceId) throws Exception {
        String routerPath = ZookeeperAssembler.buildServiceRouterPath(microServiceId, stage, routerMicroServiceId);
        try {
            byte[] bytes = client.getData().forPath(routerPath);
            return ZookeeperAssembler.toRouterData(bytes);
        } catch (Exception e) {
            throw new DashboardSystemException(e, ResponseCodeEnum.ZOOKEEPER_CLIENT_ERROR,
                String.format("getData for path %s failed", routerPath));
        }
    }

    @Override
    public void saveRoutersAndNotify(String microServiceId, String stage, String routerMicroServiceId, RpcRouterData routerData) throws Exception {
        // 生成ID
        List<RpcRouterDTO> routerDTO = RpcInvokerAssembler.toRouterDTO(routerData);
        RouterRefreshDTO routerRefreshDTO = new RouterRefreshDTO();
        routerRefreshDTO.setRouterMicroServiceId(routerMicroServiceId);
        routerRefreshDTO.setRpcRouterDTOList(routerDTO);
        InterProcessMutex lock = new InterProcessMutex(client, "/lock");
        if (lock.acquire(10, TimeUnit.SECONDS)) {
            try {
                routerRefreshDTO.setOpsGlobalId(System.currentTimeMillis());
                Thread.sleep(1);
            } finally {
                lock.release();
            }
        } else {
            throw new DashboardSystemException(ResponseCodeEnum.ZOOKEEPER_CLIENT_ERROR, "zookeeper-lock failed");
        }

        // 保存到ZK
        doRoutersSave(microServiceId, stage, routerMicroServiceId, routerData);

        // 通知
        List<RpcInvokerData> invokerDataList = listInvokers(microServiceId, stage);
        for (RpcInvokerData invokerData : invokerDataList) {
            refreshRouterExecutor.execute(() -> {
                MetaData nodeProperties = invokerData.getNodeProperties();
                try {
                    ClassicHttpRequest httpPost = ClassicRequestBuilder.post("http://" + nodeProperties.getHost() + ":9090" + ApiUrl.ROUTER_REFRESH)
                        .addHeader("Content-Type", "application/json")
                        .setEntity(mapper.writeValueAsBytes(routerRefreshDTO), ContentType.APPLICATION_JSON)
                        .build();

                    httpClient.execute(httpPost, response -> {
                        // do something useful with the response body
                        // and ensure it is fully consumed
                        log.info("notify invoker[{}] response: {},{}", nodeProperties, response.getCode(), EntityUtils.toString(response.getEntity()));
                        EntityUtils.consume(response.getEntity());
                        return (CloseableHttpResponse) response;
                    });
                } catch (IOException e) {
                    log.error("refresh router failed, target invoker[{}]", nodeProperties, e);
                }
            });
        }
    }

    private void doRoutersSave(String microServiceId, String stage, String routerMicroServiceId, RpcRouterData routerData) {
        String routerPath = ZookeeperAssembler.buildServiceRouterPath(microServiceId, stage, routerMicroServiceId);
        try {
            byte[] routerBytes = ZookeeperAssembler.toRouterBytes(routerData);

            if (client.checkExists().forPath(routerPath) == null) {
                client.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(routerPath);
            }

            Stat stat = client.setData().forPath(routerPath, routerBytes);
            log.info("save RpcRouterData for path {} success, resp={}", routerPath, stat);
        } catch (JsonProcessingException e) {
            throw new DashboardRuntimeException(e, ResponseCodeEnum.PARAMETER_INVALID,
                String.format("invalid RpcRouterData for path [%s]", routerPath));
        } catch (Exception e) {
            throw new DashboardSystemException(e, ResponseCodeEnum.ZOOKEEPER_CLIENT_ERROR,
                String.format("create router for path [%s] failed", routerPath));
        }
    }

}
