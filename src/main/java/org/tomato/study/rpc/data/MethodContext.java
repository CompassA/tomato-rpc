package org.tomato.study.rpc.data;

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
public class MethodContext {

    private String interfaceName;

    private String methodName;

    private Class<?>[] argsType;

    private Class<?> returnType;

    private Object[] parameters;

    public MethodContext(String interfaceName,
                         String methodName,
                         Class<?>[] argsType,
                         Class<?> returnType,
                         Object... parameters) {
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
