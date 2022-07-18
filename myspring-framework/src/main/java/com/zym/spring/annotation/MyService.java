package com.zym.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 自定义 Service 注解
 * @author zym
 */
@Documented
@Inherited
@Target({TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MyService {
    String value() default "";
}