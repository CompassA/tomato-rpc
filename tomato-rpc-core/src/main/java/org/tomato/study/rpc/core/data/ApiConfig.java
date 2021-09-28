package org.tomato.study.rpc.core.data;

import lombok.Getter;
import org.tomato.study.rpc.core.api.TomatoApi;

import java.util.Optional;

/**
 * rpc接口元数据
 * @author Tomato
 * Created on 2021.09.29
 */
@Getter
public class ApiConfig<T> {

    /**
     * 服务唯一标识
     */
    private final String serviceVIP;

    /**
     * rpc接口
     */
    private final Class<T> api;

    private ApiConfig(String serviceVIP, Class<T> api) {
        this.serviceVIP = serviceVIP;
        this.api = api;
    }

    public static <T> Optional<ApiConfig<T>> create(Class<T> api) {
        if (!api.isInterface()) {
            return Optional.empty();
        }
        TomatoApi apiInfo = api.getAnnotation(TomatoApi.class);
        if (apiInfo == null) {
            return Optional.empty();
        }
        return Optional.of(
                new ApiConfig<>(
                        apiInfo.serviceVIP(),
                        api
                )
        );
    }
}
