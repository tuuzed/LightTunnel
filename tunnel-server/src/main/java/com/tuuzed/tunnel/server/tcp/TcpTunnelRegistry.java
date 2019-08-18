package com.tuuzed.tunnel.server.tcp;

import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpTunnelRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TcpTunnelRegistry.class);

    @NotNull
    private final Map<Long, TcpTunnelDescriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Integer, TcpTunnelDescriptor> portDescriptors = new ConcurrentHashMap<>();


    synchronized void register(int port, @NotNull ServerTunnelSessions tunnelSessions, TcpTunnelDescriptor descriptor) {
        tunnelTokenDescriptors.put(tunnelSessions.tunnelToken(), descriptor);
        portDescriptors.put(port, descriptor);
        logger.info("Start Tunnel: {}", tunnelSessions.protoRequest());
    }

    synchronized void unregister(long tunnelToken) {
        final TcpTunnelDescriptor descriptor = tunnelTokenDescriptors.remove(tunnelToken);
        if (descriptor != null) {
            portDescriptors.remove(descriptor.port());
            descriptor.close();
            logger.info("Shutdown Tunnel: {}", descriptor.tunnelSessions().protoRequest());
        }
    }

    @Nullable
    synchronized Channel getSessionChannel(long tunnelToken, long sessionToken) {
        TcpTunnelDescriptor descriptor;
        descriptor = tunnelTokenDescriptors.get(tunnelToken);
        if (descriptor == null) {
            return null;
        }
        return descriptor.tunnelSessions().getSessionChannel(sessionToken);
    }


    @Nullable
    synchronized TcpTunnelDescriptor getDescriptorByPort(int port) {
        return portDescriptors.get(port);
    }

    synchronized void destroy() {
        tunnelTokenDescriptors.clear();
        portDescriptors.clear();
    }

}
