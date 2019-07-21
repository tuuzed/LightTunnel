package com.tuuzed.tunnel.common.protocol;

import org.jetbrains.annotations.NotNull;

public interface OpenTunnelRequestInterceptor {
    /**
     * 如需拦截则抛出异常
     */
    @NotNull
    OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException;
}
