package org.tomato.study.rpc.netty.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

/**
 * @author Tomato
 * Created on 2021.03.31
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
public class RpcRequest {

    /**
     * rpc service vip in the service name registry
     */
    private String serviceVIP;

    /**
     * RPC interface class full name,
     * rpc server maintains the mapping: interfaceName -> Server Provider Instance
     */
    private String interfaceName;

    /**
     * RPC method name
     * RPC server gets the provider instance
     * and searches the Method instance by instance.class.getMethod
     */
    private String methodName;

    /**
     * RPC method types of the arguments
     */
    private Class<?>[] argsType;

    /**
     * RPC method return type
     */
    private Class<?> returnType;

    /**
     * the arguments input from client interface
     * the order is the same as the rpc method signature
     */
    private Object[] parameters;

    public RpcRequest(String serviceVIP,
                      String interfaceName,
                      String methodName,
                      Class<?>[] argsType,
                      Class<?> returnType,
                      Object... parameters) {
        this.serviceVIP = serviceVIP;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.argsType = argsType;
        this.returnType = returnType;
        if (parameters == null || parameters.length == 0) {
            this.parameters = new Object[0];
        } else {
            this.parameters = Arrays.copyOf(parameters, parameters.length);
        }
    }
}
