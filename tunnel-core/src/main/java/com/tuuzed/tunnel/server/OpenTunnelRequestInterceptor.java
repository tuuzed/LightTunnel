package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.protocol.TunnelUri;
import org.jetbrains.annotations.NotNull;

/**
 * 开启隧道请求拦截器
 */
public interface OpenTunnelRequestInterceptor {
    void proceed(@NotNull TunnelUri tunnelUri) throws Exception;
}
