package org.tomato.study.rpc.core.spi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * marked on a interface which supports SPI
 * @author Tomato
 * Created on 2021.06.12
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpiInterface {

    String paramName() default "";

    boolean singleton() default true;
}
