package com.tuuzed.tunnel.server;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UserTunnel {
    @NotNull
    Channel serverChannel();

    int bindPort();

    void putUserTunnelChannel(long tunnelToken, long sessionToken, @NotNull Channel channel);

    @Nullable
    Channel getUserTunnelChannel(long tunnelToken, long sessionToken);

    void removeUserTunnelChannel(long tunnelToken, long sessionToken);
}
