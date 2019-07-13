package com.tuuzed.tunnel.common;

import org.jetbrains.annotations.NotNull;

public interface Interceptor<T> {
    /**
     * 如需拦截则抛出异常
     */
    boolean proceed(@NotNull T t, @NotNull String[] errorMessage);
}
