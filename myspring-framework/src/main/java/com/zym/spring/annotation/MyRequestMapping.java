package com.zym.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * 自定义 RequestMapping 注解
 * @author zym
 */
@Documented
@Inherited
@Target({TYPE, METHOD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MyRequestMapping {
    String value() default "";
}
