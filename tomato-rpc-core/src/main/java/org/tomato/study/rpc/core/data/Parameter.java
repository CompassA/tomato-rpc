package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * header extension K/V parameter
 * @author Tomato
 * Created on 2021.04.03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {

    /**
     * parameter key
     */
    private String key;

    /**
     * parameter value
     */
    private String value;
}
