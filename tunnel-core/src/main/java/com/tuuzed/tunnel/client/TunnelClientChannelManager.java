package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TunnelClientChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelManager.class);

    private static class InstanceHolder {
        private static final TunnelClientChannelManager instance = new TunnelClientChannelManager();
    }

    @NotNull
    public static TunnelClientChannelManager getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<String, Channel> mappingChannels = new ConcurrentHashMap<>();

    @Nullable
    public Channel getChannel(@NotNull String mapping) {
        return mappingChannels.get(mapping);
    }

    public void addChannel(@NotNull String mapping, @NotNull Channel channel) {
        if (mappingChannels.containsKey(mapping)) {
            throw new IllegalArgumentException("containsKey: " + mapping);
        }
        mappingChannels.put(mapping, channel);
    }

    public void removeChannel(@NotNull String mapping) {
        mappingChannels.remove(mapping);
    }


}
