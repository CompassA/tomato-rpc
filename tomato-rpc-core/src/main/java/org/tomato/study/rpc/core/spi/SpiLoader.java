package org.tomato.study.rpc.core.spi;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
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

    /**
     * lazy: [get] or [create and get] spi instance
     * @return spi instance
     */
    public T load() {
        if (!spiInterface.isAnnotationPresent(SpiInterface.class)) {
            return null;
        }
        SpiInterface spiInfo = spiInterface.getAnnotation(SpiInterface.class);
        if (!spiInfo.singleton()) {
            return createSpiInstance(spiInfo.paramName());
        }
        T instance = singleton.get();
        if (instance == null) {
            synchronized (singleton) {
                instance = singleton.get();
                if (instance == null) {
                    instance = createSpiInstance(spiInfo.paramName());
                    singleton.set(instance);
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T createSpiInstance(final String spiParameterName) {
        Map<String, Class<?>> spiConfigMap = getSpiConfigMap();
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

    private Map<String, Class<?>> loadSpiConfigFile() {
        String path = SPI_CONFIG_DICTIONARY + spiInterface.getCanonicalName();
        ClassLoader classLoader = ClassUtil.getClassLoader(spiInterface);
        if (classLoader == null) {
            return Collections.emptyMap();
        }
        URL resourceUrl = classLoader.getResource(path);
        if (resourceUrl == null) {
            return Collections.emptyMap();
        }
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
