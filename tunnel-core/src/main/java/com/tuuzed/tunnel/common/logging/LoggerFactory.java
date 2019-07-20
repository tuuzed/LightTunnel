package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public final class LoggerFactory {
    @NotNull
    public static Logger getLogger(@NotNull Class clazz) {
        return LogConfigurator.getCreator().getLogger(clazz);
    }

    @NotNull
    public static Logger getLogger(@NotNull String name) {
        return LogConfigurator.getCreator().getLogger(name);
    }


}
