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
     * path of spi config dictionary
     */
    private static final String SPI_CONFIG_DICTIONARY = "META-INF/tomato/";

    /**
     * parameterName : org.study.xxx.xx.ClassType
     */
    private static final int DELIMITER = ':';

    /**
     * interface class marked with @SpiInterface -> spi loader
     */
    private static final ConcurrentMap<Class<?>, SpiLoader<?>> LOADER_MAP = new ConcurrentHashMap<>(0);

    /**
     * interface -> wrapper class constructor
     */
    private static final ConcurrentMap<Class<?>, Constructor<?>> WRAPPER_MAP = new ConcurrentHashMap<>();

    /**
     * spi key -> implementation class
     */
    private final ObjectHolder<Map<String, Class<?>>> spiConfigHolder = new ObjectHolder<>();

    /**
     * spi implementation class singleton holder
     */
    private final ObjectHolder<T> singleton = new ObjectHolder<>();

    /**
     * interface class marked with @SpiInterface
     */
    private final Class<T> spiInterface;

    /**
     * spi config parameter name，indicate that user chose which impl class
     */
    private final String paramName;

    /**
     * default implement class name
     */
    private final String defaultClassFullName;

    /**
     * is implement class instance singleton
     */
    private final boolean singletonInstance;

    /**
     * [get] or [create and get] a spi loader of a spi interface
     * @param spiInterface interface class marked with @SpiInterface
     * @param <T> extension interface type
     * @return spi loader
     */
    @SuppressWarnings("unchecked")
    public static <T> SpiLoader<T> getLoader(Class<T> spiInterface) {
        return (SpiLoader<T>) LOADER_MAP.computeIfAbsent(spiInterface, SpiLoader::new);
    }

    /**
     * 编程式注入SPI实例
     * @param clazz 接口类型
     * @param instance 接口实例
     * @param <T> 接口类型
     */
    public static <T> void registerLoader(Class<T> clazz, T instance) {
        if (!clazz.isInterface() || !clazz.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException();
        }
        LOADER_MAP.put(clazz, new SpiLoader<>(clazz, instance));
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
     * 编程式配置SPI实例
     * @param clazz 接口类型
     * @param instance 接口实例
     */
    private SpiLoader(Class<T> clazz, T instance) {
        this.singleton.set(instance);
        this.spiInterface = clazz;
        this.paramName = "";
        this.defaultClassFullName = "";
        this.singletonInstance = true;
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
        this.paramName = "".equals(spiInfo.paramName())
                ? clazz.getSimpleName()
                : spiInfo.paramName();
        this.defaultClassFullName = spiInfo.defaultSpiValue();
        this.singletonInstance = spiInfo.singleton();
    }

    /**
     * lazy: [get] or [create and get] spi instance
     * @return spi instance
     */
    public T load() {
        if (!this.singletonInstance) {
            return this.createSpiInstance(this.paramName);
        }
        T instance = this.singleton.get();
        if (instance == null) {
            synchronized (this.singleton) {
                instance = this.singleton.get();
                if (instance == null) {
                    instance = this.createSpiInstance(this.paramName);
                    this.singleton.set(instance);
                }
            }
        }
        return instance;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private T createSpiInstance(final String spiParameterName) {
        Map<String, Class<?>> spiConfigMap = this.getSpiConfigMap();
        // 获取实现类
        Class<?> spiImplClass = spiConfigMap.get(spiParameterName);
        if (spiImplClass == null) {
            return null;
        }

        // 创建实现类实例
        final Object spiInstance = spiImplClass.getConstructor().newInstance();

        // 构建包装器
        final Constructor<?> constructor = WRAPPER_MAP.get(spiInterface);
        if (constructor != null) {
            return (T) constructor.newInstance(spiInstance);
        }

        return (T) spiInstance;
    }

    private Map<String, Class<?>> getSpiConfigMap() {
        Map<String, Class<?>> spiConfigMap = this.spiConfigHolder.get();
        if (spiConfigMap == null) {
            synchronized (this.spiConfigHolder) {
                spiConfigMap = this.spiConfigHolder.get();
                if (spiConfigMap == null) {
                    spiConfigMap = this.loadSpiConfigFile();
                    if (!spiConfigMap.isEmpty()) {
                        this.spiConfigHolder.set(spiConfigMap);
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
        // get classloader
        String path = SPI_CONFIG_DICTIONARY + this.spiInterface.getCanonicalName();
        ClassLoader classLoader = ClassUtil.getClassLoader(this.spiInterface);
        if (classLoader == null) {
            return Collections.emptyMap();
        }

        // open spi config file
        URL resourceUrl = classLoader.getResource(path);

        // if config is empty, load default implement class
        Map<String, Class<?>> spiConfigMap = new HashMap<>(0);
        if (resourceUrl == null) {
            spiConfigMap.put(
                    this.paramName,
                    Class.forName(this.defaultClassFullName, true, classLoader)
            );
            return spiConfigMap;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
            // resolve config line
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
