package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import org.jetbrains.annotations.NotNull;

class HttpTunnelDescriptor {
    @NotNull
    private final String vhost;
    @NotNull
    private final ServerTunnelSessions tunnelSessions;

    HttpTunnelDescriptor(@NotNull String vhost, @NotNull ServerTunnelSessions tunnelSessions) {
        this.vhost = vhost;
        this.tunnelSessions = tunnelSessions;
    }

    @NotNull
    public String vhost() {
        return vhost;
    }

    @NotNull
    public ServerTunnelSessions tunnelSessions() {
        return tunnelSessions;
    }

    public void close() {
        tunnelSessions.destroy();
    }

    @Override
    public String toString() {
        return "HttpTunnelDescriptor{" +
            "vhost='" + vhost + '\'' +
            ", tunnelSessions=" + tunnelSessions +
            '}';
    }
}

