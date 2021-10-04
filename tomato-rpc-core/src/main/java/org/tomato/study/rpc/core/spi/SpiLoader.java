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
import org.tomato.study.rpc.core.utils.ClassUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * load rpc extensions
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
     * 配置的键值对分隔符
     * parameterName : org.study.xxx.xx.ClassType
     */
    private static final int DELIMITER = ':';

    /**
     * SPI接口 -> SPI接口对应的类
     */
    private static final ConcurrentMap<Class<?>, SpiLoader<?>> LOADER_MAP = new ConcurrentHashMap<>(0);

    /**
     * SPI接口 -> 接口包装器
     */
    private static final ConcurrentMap<Class<?>, Constructor<?>> WRAPPER_MAP = new ConcurrentHashMap<>();

    //==================================================================================================================
    /**
     * SPI配置文件中的组件Key -> 组件全类名
     */
    private final ObjectHolder<Map<String, Class<?>>> spiConfigHolder = new ObjectHolder<>();

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
    private final String paramName;

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
     * 编程式注入SPI默认实例
     * @param clazz 接口类型
     * @param instance 接口实例
     * @param <T> 接口类型
     */
    public static <T> void registerSpiInstance(Class<T> clazz, T instance) {
        SpiLoader<T> loader = getLoader(clazz);
        if (loader == null) {
            return;
        }
        loader.getSingletonMap().put(loader.getParamName(), instance);
    }

    /**
     * 注册装饰器
     * @param interfaceClass 接口类型
     * @param wrapper 装饰器类型
     * @param <T> 接口类型
     */
    public static <T> void registerWrapper(Class<T> interfaceClass, Class<? extends T> wrapper) {
        try {
            WRAPPER_MAP.put(interfaceClass, wrapper.getConstructor(interfaceClass));
        } catch (NoSuchMethodException e) {
            // do nothing
        }
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
        this.paramName = spiInfo.value();
        this.singletonInstance = spiInfo.singleton();
    }

    /**
     * 加载key为 {@link SpiLoader#getParamName()} 对应的默认实现类
     * @return spi instance
     */
    public T load() {
        return load(paramName);
    }

    /**
     * SPI配置文件中的组件参数名
     * @param paramName 参数名
     * @return spi实例
     */
    public T load(String paramName) {
        if (!singletonInstance) {
            return createSpiInstance(paramName);
        }
        return singletonMap.computeIfAbsent(paramName, this::createSpiInstance);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private T createSpiInstance(final String spiParameterName) {
        // 加载配置文件，转为Map
        Map<String, Class<?>> spiConfigMap = getSpiConfigMap();

        // 获取实现类
        Class<?> spiImplClass = spiConfigMap.get(spiParameterName);
        if (spiImplClass == null) {
            return null;
        }

        // 创建实现类实例
        Object spiInstance = spiImplClass.getConstructor().newInstance();

        // 如果该类对象有包装器，构建包装器
        Constructor<?> constructor = WRAPPER_MAP.get(spiInterface);
        if (constructor != null) {
            return (T) constructor.newInstance(spiInstance);
        }
        return (T) spiInstance;
    }

    private Map<String, Class<?>> getSpiConfigMap() {
        Map<String, Class<?>> spiConfigMap = spiConfigHolder.get();
        if (spiConfigMap == null) {
            synchronized (spiConfigHolder) {
                spiConfigMap = spiConfigHolder.get();
                if (spiConfigMap == null) {
                    spiConfigMap = loadSpiConfigFile();
                    if (!spiConfigMap.isEmpty()) {
                        spiConfigHolder.set(spiConfigMap);
                    }
                }
            }
        }
        return spiConfigMap;
    }

    /**
     * load spi config file
     * @return {@link SpiLoader#paramName} -> implement class of spi interface
     */
    @SneakyThrows
    private Map<String, Class<?>> loadSpiConfigFile() {
        // 获取类加载器
        String path = SPI_CONFIG_DICTIONARY + spiInterface.getCanonicalName();
        ClassLoader classLoader = ClassUtil.getClassLoader(spiInterface);
        if (classLoader == null) {
            return Collections.emptyMap();
        }

        // 打开SPI配置文件
        URL resourceUrl = classLoader.getResource(path);
        if (resourceUrl == null) {
            return Collections.emptyMap();
        }

        // 一行一行解析文件，存入Map[组件key -> 组件类型]
        Map<String, Class<?>> spiConfigMap = new HashMap<>(0);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                int delimiterPos = line.indexOf(DELIMITER);
                if (delimiterPos < 1) {
                    continue;
                }

                // load class by class full name in the config file
                String paramName = line.substring(0, delimiterPos).trim();
                String implClassName = line.substring(delimiterPos + 1).trim();
                if (paramName.isEmpty() || implClassName.isEmpty()) {
                    continue;
                }
                Class<?> clazz = Class.forName(implClassName, true, classLoader);
                spiConfigMap.put(paramName, clazz);
            }
        }
        return spiConfigMap;
    }
}
