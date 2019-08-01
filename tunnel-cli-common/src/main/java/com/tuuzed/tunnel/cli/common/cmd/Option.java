package com.tuuzed.tunnel.cli.common.cmd;

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

    int order() default 0;


}
