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
import org.tomato.study.rpc.core.utils.ClassUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
     * spi config parameter name
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
        SpiLoader<?> spiLoader = LOADER_MAP.get(spiInterface);
        if (spiLoader == null) {
            LOADER_MAP.putIfAbsent(spiInterface, new SpiLoader<>(spiInterface));
            spiLoader = LOADER_MAP.get(spiInterface);
        }
        return (SpiLoader<T>) spiLoader;
    }

    public SpiLoader(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(SpiInterface.class)) {
            throw new IllegalStateException("not spi interface");
        }
        SpiInterface spiInfo = clazz.getAnnotation(SpiInterface.class);
        this.spiInterface = clazz;
        this.paramName = spiInfo.paramName();
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

    @SuppressWarnings("unchecked")
    private T createSpiInstance(final String spiParameterName) {
        Map<String, Class<?>> spiConfigMap = this.getSpiConfigMap();
        Class<?> spiImplClass = spiConfigMap.get(spiParameterName);
        if (spiImplClass == null) {
            return null;
        }
        try {
            return (T) spiImplClass.getConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
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
            try {
                spiConfigMap.put(
                        this.paramName,
                        Class.forName(this.defaultClassFullName, true, classLoader)
                );
                return spiConfigMap;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
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
                try {
                    Class<?> clazz = Class.forName(implClassName, true, classLoader);
                    spiConfigMap.put(paramName, clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return spiConfigMap;
    }
}
