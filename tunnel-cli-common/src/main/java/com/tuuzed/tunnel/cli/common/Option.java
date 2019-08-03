package com.tuuzed.tunnel.cli.common;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
    @NotNull
    String name();

    @NotNull
    String longName() default "";

    @NotNull
    String help() default "";

    // 类型名称
    @NotNull
    String typeName() default "";

    // 排序，值越小排在越前面
    int order() default 0;

    // 排除的枚举对象
    @NotNull
    String[] excludeEnums() default {};


}
