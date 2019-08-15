package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.util.PortUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultProtoRequestInterceptor implements ProtoRequestInterceptor {
    @NotNull
    private final String presetToken;
    @Nullable
    private final String portRule;

    public DefaultProtoRequestInterceptor(@NotNull String token, @Nullable String portRule) {
        this.portRule = portRule;
        this.presetToken = token;
    }

    @NotNull
    @Override
    public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
        String token = request.token();
        if (!presetToken.equals(token)) {
            throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
        }
        if (request.isTcp()) {
            final int remotePort = request.remotePort();
            if (portRule != null && !PortUtils.inPortRule(portRule, remotePort)) {
                throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
            }
            return request;
        }
        return request;
    }


}
