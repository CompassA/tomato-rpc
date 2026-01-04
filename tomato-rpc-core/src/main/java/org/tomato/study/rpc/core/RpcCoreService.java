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

package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.invoker.RpcInvokerFactory;
import org.tomato.study.rpc.core.observer.LifeCycle;
import org.tomato.study.rpc.core.registry.NameServer;

import java.net.URI;
import java.util.List;

/**
 * core method of rpc server
 * @author Tomato
 * Created on 2021.03.30
 */
public interface RpcCoreService extends LifeCycle {

    /**
     * register service provider
     * @param serviceInstance service bean
     * @param serviceInterface service interface class
     * @param <T> service type
     * @return service address
     */
    <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface);

    /**
     * create client proxy consumer
     * @param stubConfig stub config
     * @param <T> consumer class type
     * @return proxy instance
     */
    <T> T createStub(StubConfig<T> stubConfig);

    /**
     * get rpc invoker factory
     * @return rpc invoker factory
     */
    RpcInvokerFactory getRpcInvokerFactory();

    /**
     * get rpc name server
     * @return name server
     */
    NameServer getNameServer();

    /**
     * is rpc core service ready;
     * @return true ready
     */
    boolean isReady();

    /**
     * get micro-service-id
     * @return service unique id
     */
    String getMicroServiceId();

    /**
     * get subscribed micro-service-id
     * @return subscribed micro-service-id list
     */
    List<String> getSubscribedServices();

    /**
     * get service stage
     * @return stage
     */
    String getStage();

    /**
     * get service group
     * @return service provider node group
     */
    String getGroup();

    /**
     * get rpc network protocol
     * @return network protocol
     */
    String getProtocol();

    /**
     * get rpc server port
     * @return rpc server port
     */
    int getPort();

    /**
     * 更新服务端属性
     * @param property 要更新的属性
     * @throws Exception 异常
     */
    void updateServerProperty(MetaData.NodeProperty property) throws Exception;

    /**
     * 获取服务端配置总线领域模型
     * @return MetaData
     */
    MetaData getRpcServerMetaData();
}
