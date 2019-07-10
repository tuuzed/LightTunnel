package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.ATTR_MAPPING;
import static com.tuuzed.tunnel.common.protocol.TunnelConstants.ATTR_REMOTE_PORT;


public final class UserTunnelManager {

    private static final Logger logger = LoggerFactory.getLogger(UserTunnelManager.class);

    private static class InstanceHolder {
        private static final UserTunnelManager instance = new UserTunnelManager();
    }

    @NotNull
    public static UserTunnelManager getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<Channel, UserTunnel> channelTunnels = new ConcurrentHashMap<>();
    private final Map<Integer, UserTunnel> portTunnels = new ConcurrentHashMap<>();


    public boolean hasTunnel(int port) {
        return portTunnels.containsKey(port);
    }

    @Nullable
    public UserTunnel getTunnel(@NotNull Channel channel) {
        return channelTunnels.get(channel);
    }

    @Nullable
    public UserTunnel getTunnel(int port) {
        return portTunnels.get(port);
    }

    public void openTunnel(@NotNull UserTunnel tunnel) {
        if (channelTunnels.containsKey(tunnel.serverChannel())) {
            throw new IllegalArgumentException("containsKey: " + tunnel.serverChannel());
        }
        tunnel.open();
        channelTunnels.put(tunnel.serverChannel(), tunnel);
        portTunnels.put(tunnel.serverChannel().attr(ATTR_REMOTE_PORT).get(), tunnel);
    }

    public void closeTunnel(@NotNull Channel channel) {
        UserTunnel tunnel = channelTunnels.remove(channel);
        portTunnels.remove(channel.attr(ATTR_REMOTE_PORT).get());
        if (tunnel != null) {
            tunnel.close();
            logger.info("Close Tunnel: {}", channel.attr(ATTR_MAPPING));
        }
    }

}

