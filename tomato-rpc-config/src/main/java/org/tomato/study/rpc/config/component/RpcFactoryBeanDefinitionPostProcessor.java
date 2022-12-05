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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.tomato.study.rpc.config.data.ClientStubMetadata;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Tomato
 * Created on 2022.12.04
 */
@Slf4j
public class RpcFactoryBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<ClientStubMetadata<?>> stubMetaSet = new HashSet<>();
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
            // 找到成员变量标注了@RpcClientStub的BeanDefinition
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (StringUtils.isBlank(beanClassName)) {
                continue;
            }
            Class<?> clazz = null;
            try {
                clazz = ClassUtils.forName(beanClassName, this.getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                log.error("bean definition class noe found, class name: {}", beanClassName);
                continue;
            }

            // 创建FactoryBean
            while (!Object.class.equals(clazz)) {
                ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
                    @Override
                    public void doWith(Field field) throws IllegalArgumentException {
                        ClientStubMetadata.create(field).ifPresent(stubMeta -> {
                            if (stubMetaSet.contains(stubMeta)) {
                                return;
                            }
                            stubMetaSet.add(stubMeta);

                            BeanDefinition stubFactoryBeanDefinition = StubFactoryBean.createBeanDefinition(stubMeta);
                            String stubUniqueKey = stubMeta.uniqueKey();
                            registry.registerBeanDefinition(stubUniqueKey, stubFactoryBeanDefinition);
                        });
                    }
                });
                clazz = clazz.getSuperclass();
            }

        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
