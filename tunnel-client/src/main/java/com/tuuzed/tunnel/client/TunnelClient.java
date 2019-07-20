package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import org.jetbrains.annotations.NotNull;

public interface TunnelClient {
    @NotNull
    String getServerAddr();

    int getServerPort();

    @NotNull
    OpenTunnelRequest getRequest();

     void shutdown();
}
