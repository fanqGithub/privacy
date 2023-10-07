package com.gzik.privacy.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要替换的方法注解器
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface PrivacyMethodReplace {
    Class oriClass();

    String oriMethod() default "";

    int oriAccess() default AsmMethodOpcodes.INVOKESTATIC;
}