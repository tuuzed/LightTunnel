package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequestInterceptor;
import com.tuuzed.tunnel.common.protocol.TunnelProtocolException;
import com.tuuzed.tunnel.common.util.PortUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenTunnelRequestInterceptorImpl implements OpenTunnelRequestInterceptor {
    @NotNull
    private final String token;
    @Nullable
    private final String portRule;

    public OpenTunnelRequestInterceptorImpl(@NotNull String token, @Nullable String portRule) {
        this.portRule = portRule;
        this.token = token;
    }

    @NotNull
    @Override
    public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
        String token = request.getOption("token");
        if (!this.token.equals(token)) {
            throw new TunnelProtocolException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
        }
        final int remotePort = request.getRemotePort();
        if (portRule != null && !PortUtils.inPortRule(portRule, remotePort)) {
            throw new TunnelProtocolException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
        }
        return request;
    }
}
