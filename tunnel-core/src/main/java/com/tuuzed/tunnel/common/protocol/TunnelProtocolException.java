package com.tuuzed.tunnel.common.protocol;

import org.jetbrains.annotations.NotNull;

public class TunnelProtocolException extends Exception {
    public TunnelProtocolException(@NotNull String message) {
        super(message);
    }

    public TunnelProtocolException(@NotNull String message, Throwable cause) {
        super(message, cause);
    }
}
