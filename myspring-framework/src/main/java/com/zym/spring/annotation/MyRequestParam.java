package com.zym.spring.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;

/**
 * 自定义 RequestParam 注解
 * @author zym
 */
@Target({PARAMETER})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MyRequestParam {
    String value() default "";
}
