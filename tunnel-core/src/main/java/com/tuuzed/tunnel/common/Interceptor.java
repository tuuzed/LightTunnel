package com.tuuzed.tunnel.common;

import org.jetbrains.annotations.NotNull;

public interface Interceptor<T> {
    /**
     * 如需拦截则抛出异常
     */
    @NotNull
    T proceed(@NotNull T t) throws TunnelProtocolException;
}
