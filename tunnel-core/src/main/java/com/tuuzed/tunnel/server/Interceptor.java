package com.tuuzed.tunnel.server;

import org.jetbrains.annotations.NotNull;

public interface Interceptor<T> {
    void proceed(@NotNull T t) throws Exception;
}
