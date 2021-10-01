package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 注册中心配置
 * @author Tomato
 * Created on 2021.09.27
 */
@Getter
@AllArgsConstructor
public class NameServerConfig {

    /**
     * 注册中心的地址
     */
    private final String connString;

    /**
     * 注册中心编解码
     */
    private final Charset charset;

    /**
     * 当前RPC node的环境，name server会订阅同环境的其余provider
     */
    private final String stage;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String connString;
        private Charset charset = StandardCharsets.UTF_8;
        private String stage = "default";

        public Builder connString(String connString) {
            this.connString = connString;
            return this;
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public NameServerConfig build() {
            return new NameServerConfig(connString, charset, stage);
        }
    }
}
