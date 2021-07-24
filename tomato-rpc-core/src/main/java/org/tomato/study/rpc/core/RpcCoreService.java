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

import java.io.Closeable;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * core method of rpc server
 * @author Tomato
 * Created on 2021.03.30
 */
public interface RpcCoreService extends Closeable {

    /**
     * create rpc server
     * @param port bind port
     * @throws Exception start exception
     */
    void startRpcServer(int port) throws Exception;

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
     * @param serviceVIP service virtual ip
     * @param serviceInterface consumer interface class
     * @param <T> consumer class type
     * @return proxy instance
     */
    <T> T createStub(String serviceVIP, Class<T> serviceInterface);

    /**
     * subscribe service
     * @param vipList vip to subscribe
     * @exception Exception subscribe exception
     */
    void subscribe(Collection<String> vipList) throws Exception;

    /**
     * get service virtual ip
     * @return service virtual ip
     */
    String getServiceVIP();

    /**
     * get subscribed vip
     * @return subscribed vip list
     */
    List<String> getSubscribedVIP();

    /**
     * get service stage
     * @return stage
     */
    String getStage();

    /**
     * get service version
     * @return service provider node version
     */
    String getVersion();
}
