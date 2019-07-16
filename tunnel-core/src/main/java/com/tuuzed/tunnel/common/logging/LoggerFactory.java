package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public final class LoggerFactory {
    @NotNull
    public static Logger getLogger(@NotNull Class clazz) {
        return getLogger(clazz.getCanonicalName());
    }

    @NotNull
    public static Logger getLogger(@NotNull String name) {
        return new DefaultLogger(name);
    }


}
