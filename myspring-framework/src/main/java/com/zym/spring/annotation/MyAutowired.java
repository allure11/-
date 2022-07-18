package com.zym.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 自定义 Autowired 注解
 * @author zym
 */
@Documented
@Inherited
@Target({TYPE, FIELD,METHOD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MyAutowired {
    String value() default "";
}
