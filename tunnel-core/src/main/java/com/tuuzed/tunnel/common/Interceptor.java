package com.tuuzed.tunnel.common;

import org.jetbrains.annotations.NotNull;

public interface Interceptor<T> {
    /**
     * 如需拦截则抛出异常
     */
    void proceed(@NotNull T t) throws Exception;
}
