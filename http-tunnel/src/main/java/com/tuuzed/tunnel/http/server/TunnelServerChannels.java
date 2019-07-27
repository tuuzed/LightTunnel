package com.tuuzed.tunnel.http.server;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TunnelServerChannels {

    private final Map<String, Channel> cachedChannels = new ConcurrentHashMap<>();

    public void putChannel(@NotNull String subDomain, @NotNull Channel tunnelServerChannel) {
        cachedChannels.put(subDomain, tunnelServerChannel);
    }

    @Nullable
    public Channel getChannel(@NotNull String subDomain) {
        return cachedChannels.get(subDomain);
    }

    @Nullable
    public Channel removeChannel(@NotNull String subDomain) {
        return cachedChannels.remove(subDomain);
    }


}
