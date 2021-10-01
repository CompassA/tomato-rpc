package org.tomato.study.rpc.core.router;

import lombok.Getter;
import org.tomato.study.rpc.core.data.MetaData;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建一个Invoker需要的数据
 * @author Tomato
 * Created on 2021.10.01
 */
public class InvokerConfig {

    /**
     * 一个实例节点的ip、端口等信息
     */
    @Getter
    private final MetaData nodeInfo;

    /**
     * 创建Invoker时的自定义参数
     */
    private final Map<String, Object> customParameter;

    private InvokerConfig(MetaData nodeInfo, Map<String, Object> customParameter) {
        this.nodeInfo = nodeInfo;
        this.customParameter = customParameter;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        return (T) customParameter.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MetaData nodeInfo;
        private final Map<String, Object> parameterMap = new HashMap<>(0);

        public Builder nodeInfo(MetaData nodeInfo) {
            this.nodeInfo = nodeInfo;
            return this;
        }

        public Builder parameter(String key, Object value) {
            parameterMap.put(key, value);
            return this;
        }

        public InvokerConfig build() {
            return new InvokerConfig(nodeInfo, parameterMap);
        }
    }
}
