package com.tuuzed.tunnel.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TunnelClientBuilder {
    @Nullable
    TunnelClientListener listener;
    boolean autoReconnect = true;
    int workerThreads = -1;

    @NotNull
    public TunnelClientBuilder setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    @NotNull
    public TunnelClientBuilder setListener(@Nullable TunnelClientListener listener) {
        this.listener = listener;
        return this;
    }

    @NotNull
    public TunnelClientBuilder setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    @NotNull
    public TunnelClient build() {
        return new TunnelClient(this);
    }
}
