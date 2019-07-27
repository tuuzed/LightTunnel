package com.tuuzed.tunnel.http.server;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServerChannels {

    private final Map<String, Channel> cachedChannels = new ConcurrentHashMap<>();

    public void putChannel(long tunnelToken, long sessionToken, @NotNull Channel tunnelServerChannel) {
        cachedChannels.put(getCachedChannelKey(tunnelToken, sessionToken), tunnelServerChannel);
    }

    @Nullable
    public Channel getChannel(long tunnelToken, long sessionToken) {
        return cachedChannels.get(getCachedChannelKey(tunnelToken, sessionToken));
    }

    @Nullable
    public Channel removeChannel(long tunnelToken, long sessionToken) {
        return cachedChannels.remove(getCachedChannelKey(tunnelToken, sessionToken));
    }

    private String getCachedChannelKey(long tunnelToken, long sessionToken) {
        return String.format("%d-%d", tunnelToken, sessionToken);
    }
}
