package org.tomato.study.rpc.netty.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomato.study.rpc.core.Invocation;

/**
 * 一次RPC调用的请求参数
 * @author Tomato
 * Created on 2021.09.30
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequestDTO implements Invocation {

    /**
     * {@link RpcRequestModel#getServiceVIP()}
     */
    private String serviceVIP;

    /**
     * {@link RpcRequestModel#getRpcInterFace()}
     */
    private String interfaceName;

    /**
     * {@link RpcRequestModel#getMethodName()}
     */
    private String methodName;

    /**
     * {@link RpcRequestModel#getArgsType()}
     */
    private String[] argsTypes;

    /**
     * {@link RpcRequestModel#getReturnType()}
     */
    private String returnType;

    /**
     * {@link RpcRequestModel#getParameters()}
     */
    private Object[] args;
}
