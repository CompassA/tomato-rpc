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

package org.tomato.study.rpc.config.component;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.tomato.study.rpc.common.utils.Logger;
import org.tomato.study.rpc.config.data.ClientStubMetadata;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.StubConfig;

/**
 * 创建StubProxyBean
 * @author Tomato
 * Created on 2022.11.30
 */
@Getter
@Setter
public class StubFactoryBean implements FactoryBean {

    private RpcCoreService rpcCoreService;
    private ClientStubMetadata<?> metaData;

    @Override
    public Object getObject() throws Exception {
        StubConfig<?> stubConfig = new StubConfig<>(
                metaData.getStubClass(),
                metaData.getMicroServiceId(),
                StringUtils.isNotBlank(metaData.getGroup()) ? metaData.getGroup() : rpcCoreService.getGroup(),
                metaData.isCompressBody(),
                metaData.getTimeout(),
                rpcCoreService.getNameServer());
        Object stub = rpcCoreService.createStub(stubConfig);
        Logger.DEFAULT.info("create stub bean: {}", stubConfig);
        return stub;
    }

    @Override
    public Class<?> getObjectType() {
        return metaData == null ? null : metaData.getStubClass();
    }

    public static BeanDefinition createBeanDefinition(ClientStubMetadata<?> metaData) {
        return BeanDefinitionBuilder.genericBeanDefinition(StubFactoryBean.class)
                .addPropertyValue("metaData", metaData)
                .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                .addAutowiredProperty("rpcCoreService")
                .getBeanDefinition();
    }
}
