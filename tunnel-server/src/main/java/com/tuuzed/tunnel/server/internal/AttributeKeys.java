package com.tuuzed.tunnel.server.internal;

import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

public final class AttributeKeys {

    @NotNull
    public static final AttributeKey<Long> SESSION_TOKEN = AttributeKey.newInstance("$SESSION_TOKEN");
    @NotNull
    public static final AttributeKey<String> VHOST = AttributeKey.newInstance("$VHOST");
    @NotNull
    public static final AttributeKey<ServerTunnelSessions> SERVER_TUNNEL_SESSIONS
        = AttributeKey.newInstance("$SERVER_TUNNEL_SESSIONS");


}
