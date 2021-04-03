package org.tomato.study.rpc.data;

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
@NoArgsConstructor
public class MethodContext {

    private String interfaceName;

    private String methodName;

    private Object[] parameters;

    public MethodContext(String interfaceName, String methodName, Object... parameters) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        if (parameters == null || parameters.length == 0) {
            this.parameters = new Object[0];
        } else {
            this.parameters = Arrays.copyOf(parameters, parameters.length);
        }
    }
}
