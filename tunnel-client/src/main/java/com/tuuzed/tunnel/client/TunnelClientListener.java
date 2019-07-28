package com.tuuzed.tunnel.client;

import org.jetbrains.annotations.NotNull;

public interface TunnelClientListener {
    void onConnecting(@NotNull TunnelClient.Descriptor descriptor, boolean reconnect);

    void onConnected(@NotNull TunnelClient.Descriptor descriptor);

    void onDisconnect(@NotNull TunnelClient.Descriptor descriptor, boolean fatal);
}