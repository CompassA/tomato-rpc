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

package org.tomato.study.rpc.core.registry;

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RefreshInvokerTask;
import org.tomato.study.rpc.core.observer.LifeCycle;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;
import java.util.Optional;

/**
 * register service and routing
 * @author Tomato
 * Created on 2021.03.31
 */
public interface NameServer extends LifeCycle {

    /**
     * get micro-service object
     * @param serviceId service id
     * @return MicroServiceSpace
     */
    Optional<MicroServiceSpace> getMicroService(String serviceId);

    /**
     * register micro service
     * @param metaData service identification、service address
     * @throws Exception register exception
     */
    void registerService(MetaData metaData) throws Exception;

    /**
     * unregister micro service
     * @param metaData service identification、service address
     * @throws Exception register exception
     */
    void unregisterService(MetaData metaData) throws Exception;

    /**
     * subscribe micro service
     * @param microServices micro services to subscribe
     * @param stage client subscribe the provider invoker which has the same stage
     * @exception Exception subscribe exception
     */
    void subscribe(MicroServiceSpace[] microServices, String stage) throws Exception;

    /**
     * unsubscribe micro service
     * @param microServices micro services to unsubscribe
     * @param stage client subscribe the provider invoker which has the same stage
     * @exception Exception subscribe exception
     */
    void unsubscribe(MicroServiceSpace[] microServices, String stage) throws Exception;

    /**
     * search service invoker
     * @param microServiceId target micro-service id
     * @param group target service group
     * @param invocation rpc invocation request data
     * @return provider invoker
     */
    Optional<RpcInvoker> lookupInvoker(String microServiceId, String group, Invocation invocation);

    /**
     * list rpc invokers of a service
     * @param microServiceId rpc service id
     * @return invokers
     */
    List<RpcInvoker> listInvokers(String microServiceId);

    /**
     * 创建更新invoker的任务
     * @param task 任务
     * @throws InterruptedException 方法是可能会阻塞的
     */
    void submitInvokerRefreshTask(RefreshInvokerTask task) throws InterruptedException;
}
