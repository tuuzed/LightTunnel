package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.util.PortUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultProtoRequestInterceptor implements ProtoRequestInterceptor {
    @NotNull
    private final String token;
    @Nullable
    private final String portRule;

    public DefaultProtoRequestInterceptor(@NotNull String token, @Nullable String portRule) {
        this.portRule = portRule;
        this.token = token;
    }


    @NotNull
    @Override
    public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
        if (request.isTcp()) {
            String token = request.option("token");
            if (!this.token.equals(token)) {
                throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
            }
            final int remotePort = request.remotePort();
            if (portRule != null && !PortUtils.inPortRule(portRule, remotePort)) {
                throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
            }
            return request;
        }
        return request;
    }


}
