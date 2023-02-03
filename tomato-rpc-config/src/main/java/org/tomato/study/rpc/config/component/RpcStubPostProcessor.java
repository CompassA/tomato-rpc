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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.tomato.study.rpc.config.annotation.RpcServerStub;
import org.tomato.study.rpc.config.data.ClientStubMetadata;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.api.TomatoApi;

import java.lang.reflect.Field;

/**
 * TomatoRpc后置处理器，创建所有stub
 * @author Tomato
 * Created on 2021.11.20
 */
@Slf4j
public class RpcStubPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private final RpcCoreService rpcCoreService;
    private BeanFactory beanFactory;

    public RpcStubPostProcessor(RpcCoreService rpcCoreService) {
        this.rpcCoreService = rpcCoreService;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 为客户端注入stub
        injectClientStub(bean);

        // 为服务端注入stub
        registerServerStub(bean);

        return bean;
    }

    @SuppressWarnings("all")
    private void registerServerStub(Object bean) {
        Class<?> clazz = bean.getClass();
        RpcServerStub rpcServerStub = clazz.getAnnotation(RpcServerStub.class);
        if (rpcServerStub == null) {
            return;
        }

        // 不断向上遍历类型，直到找到标注了@TomatoApi的接口
        while (!Object.class.equals(clazz)) {
            for (Class<?> interfaceClazz : clazz.getInterfaces()) {
                TomatoApi tomatoApi = interfaceClazz.getAnnotation(TomatoApi.class);
                if (tomatoApi != null) {
                    rpcCoreService.registerProvider(bean, (Class<Object>) interfaceClazz);
                }
            }
            clazz = clazz.getSuperclass();
        }

    }

    private void injectClientStub(Object bean) {
        Class<?> clazz = bean.getClass();
        while (!Object.class.equals(clazz)) {
            // 扫描所有类型，找到标注了@RpcClientStub的成员变量
            for (Field field : clazz.getDeclaredFields()) {
                ClientStubMetadata.create(field)
                        .ifPresent(metaData -> injectStubField(bean, field, metaData));
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void injectStubField(Object bean, Field field, ClientStubMetadata<?> metaData) {
        String uniqueKey = metaData.uniqueKey();
        Object stub = beanFactory.getBean(uniqueKey);
        try {
            field.setAccessible(true);
            field.set(bean, stub);
        } catch (IllegalAccessException e) {
            log.error("create stub error", e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
