package org.tomato.study.rpc.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * header extension parameter
 * @author Tomato
 * Created on 2021.04.03
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {

    private String key;

    private String value;
}
