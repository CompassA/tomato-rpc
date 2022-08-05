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

package org.tomato.study.rpc.core.spi;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.io.FileStreamResource;
import org.tomato.study.rpc.core.io.UrlFileStreamResource;
import org.tomato.study.rpc.core.utils.ClassUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 加载SPI实例
 * @author Tomato
 * Created on 2021.04.17
 */
@Getter
public class SpiLoader<T> {

    /**
     * SPI配置文件路径
     */
    private static final String SPI_CONFIG_DICTIONARY = "META-INF/tomato/";

    /**
     * set方法前缀
     */
    private static final String SETTER_PREFIX = "set";

    /**
     * 配置的键值对分隔符
     * parameterName : org.study.xxx.xx.ClassType
     */
    private static final int DELIMITER = ':';

    /**
     * SPI接口 -> SPI接口对应的类
     */
    private static final ConcurrentMap<Class<?>, SpiLoader<?>> LOADER_MAP = new ConcurrentHashMap<>(0);

    /**
     * 用户通过jvm参数指定的SPI组件
     * 接口名 -> 参数名
     */
    private static Map<String, String> JVM_PRIORITY_CONFIG = RpcJvmConfigKey.parseMultiKeyValue(
            System.getProperty(RpcJvmConfigKey.SPI_CUSTOM_CONFIG));

    //==================================================================================================================
    /**
     * SPI配置文件中的组件Key -> 组件全类名
     */
    private final Map<String, Class<? extends T>> componentMap;

    /**
     * 组件单例Map[SPI配置中的key -> 组件单例]
     */
    private final ConcurrentMap<String, T> singletonMap = new ConcurrentHashMap<>(0);

    /**
     * SPI接口
     */
    private final Class<T> spiInterface;

    /**
     * 接口配置的默认实现参数
     */
    private final String defaultKey;

    /**
     * 是否是单例
     */
    private final boolean singletonInstance;

    /**
     * 得到一个SPI接口的加载器，采用懒加载，第一次要使用一个SPI接口加载器时才去初始化
     * @param spiInterface 被标记@SpiInterface的接口
     * @param <T> SPI接口类型
     * @return spi loader
     */
    @SuppressWarnings("unchecked")
    public static <T> SpiLoader<T> getLoader(Class<T> spiInterface) {
        return (SpiLoader<T>) LOADER_MAP.computeIfAbsent(spiInterface, SpiLoader::new);
    }

    /**
     * 暴露接口重置SPI组件配置
     */
    public static void resetPriorityMap() {
        JVM_PRIORITY_CONFIG = RpcJvmConfigKey.parseMultiKeyValue(
                System.getProperty(RpcJvmConfigKey.SPI_CUSTOM_CONFIG));
    }

    /**
     * 注解式SPI
     * @param clazz 接口类型
     */
    public SpiLoader(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(SpiInterface.class)) {
            throw new IllegalArgumentException("not spi interface");
        }
        SpiInterface spiInfo = clazz.getAnnotation(SpiInterface.class);
        this.spiInterface = clazz;
        this.defaultKey = spiInfo.value();
        this.singletonInstance = spiInfo.singleton();
        this.componentMap = loadSpiConfigFile();
    }

    /**
     * 加载用户指定的SPI组件，若未指定，加载注解指定的默认参数
     * @return spi instance
     */
    public T load(Object... args) {
        String priorityKey = JVM_PRIORITY_CONFIG.get(spiInterface.getCanonicalName());
        if (StringUtils.isNotBlank(priorityKey)) {
            T component = load(priorityKey, args);
            if (component != null) {
                return component;
            }
        }
        return load(defaultKey, args);
    }

    /**
     * SPI配置文件中的组件参数名
     * @param paramName 参数名
     * @return spi实例
     */
    public T load(String paramName, Object... args) {
        // 获取实现类
        Class<? extends T> spiImplClass = componentMap.get(paramName);
        if (spiImplClass == null) {
            return null;
        }
        if (!singletonInstance) {
            return createSpiInstance(spiImplClass, args);
        }
        // 加锁，防止把未完成依赖注入的不完整对象暴露
        synchronized (paramName.intern()) {
            T component = singletonMap.get(paramName);
            if (component == null) {
                // 创建spi实例, 并在注入依赖前，提前加入map，防止循环依赖
                component = singletonMap.computeIfAbsent(paramName, key -> createSpiInstance(spiImplClass, args));
                // 注入依赖的其余spi组件
                injectSpiComponents(spiImplClass, component);
            }
            return component;
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private T createSpiInstance(Class<? extends T> spiImplClass, Object... args) {
        Object spiInstance = null;
        if (args.length > 0) {
            for (Constructor<?> constructor : spiImplClass.getConstructors()) {
                if (constructor.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    spiInstance = constructor.newInstance(args);
                    break;
                } catch (Exception e) {
                    // do nothing
                }
            }

        } else {
            // 创建实现类实例(实现类需要有无参构造函数)
            spiInstance = spiImplClass.getConstructor().newInstance();
        }
        return (T) spiInstance;
    }

    @SneakyThrows
    private void injectSpiComponents(Class<? extends T> spiImplClass, T component) {
        for (Method method : spiImplClass.getDeclaredMethods()) {
            if (!isSetter(method)) {
                continue;
            }
            Class<?> spiInterface = method.getParameterTypes()[0];
            if (spiInterface.isInterface() && spiInterface.isAnnotationPresent(SpiInterface.class)) {
                method.invoke(component,
                        SpiLoader.getLoader(spiInterface).load());
            }
        }
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith(SETTER_PREFIX)
                && method.getParameterCount() == 1
                && Modifier.isPublic(method.getModifiers());
    }

    /**
     * load spi config file
     * @return 配置文件中的组件key -> 具体实现类
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends T>> loadSpiConfigFile() {
        // 获取类加载器
        String path = SPI_CONFIG_DICTIONARY + spiInterface.getCanonicalName();
        ClassLoader classLoader = ClassUtil.getClassLoader(spiInterface);
        if (classLoader == null) {
            return Collections.emptyMap();
        }

        // 打开SPI配置文件,一行一行解析文件，存入Map[组件key -> 组件类型]
        Map<String, Class<? extends T>> spiConfigMap = new HashMap<>(0);
        Enumeration<URL> resources = classLoader.getResources(path);
        while (resources.hasMoreElements()) {
            FileStreamResource resource = new UrlFileStreamResource(resources.nextElement());
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.openNewStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    int delimiterPos = line.indexOf(DELIMITER);
                    if (delimiterPos < 1) {
                        continue;
                    }

                    // 获取参数名与全类名
                    String paramName = line.substring(0, delimiterPos).trim();
                    String implClassName = line.substring(delimiterPos + 1).trim();
                    if (paramName.isEmpty() || implClassName.isEmpty()) {
                        continue;
                    }
                    Class<?> clazz = Class.forName(implClassName, true, classLoader);
                    if (spiInterface.isAssignableFrom(clazz)) {
                        spiConfigMap.put(paramName, (Class<? extends T>) clazz);
                    }
                }
            }
        }
        return spiConfigMap;
    }
}
