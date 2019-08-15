package com.tuuzed.tunnel.client;

import org.jetbrains.annotations.NotNull;

public interface TunnelClientListener {
    void onConnecting(@NotNull TunnelClientDescriptor descriptor, boolean reconnect);

    void onConnected(@NotNull TunnelClientDescriptor descriptor);

    void onDisconnect(@NotNull TunnelClientDescriptor descriptor, boolean fatal);
}