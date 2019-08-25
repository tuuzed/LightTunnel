package com.tuuzed.tunnel.util;

import org.jetbrains.annotations.NotNull;

public interface Function1<T> {
    void invoke(@NotNull T t) throws Exception;
}
