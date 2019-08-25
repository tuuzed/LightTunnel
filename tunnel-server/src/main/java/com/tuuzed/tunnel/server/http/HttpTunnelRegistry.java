package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.proto.ProtoException;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpTunnelRegistry {
    private static final Logger logger = LoggerFactory.getLogger(HttpTunnelRegistry.class);
    @NotNull
    private final Map<Long, HttpTunnelDescriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, HttpTunnelDescriptor> vhostDescriptors = new ConcurrentHashMap<>();

    /* package */
    synchronized boolean isRegistered(@NotNull String vhost) {
        return vhostDescriptors.containsKey(vhost);
    }

    /* package */
    synchronized void register(
        @NotNull String vhost,
        @NotNull ServerTunnelSessions sessions
    ) throws ProtoException {
        if (isRegistered(vhost)) {
            throw new ProtoException("vhost(" + vhost + ") already used");
        }
        final HttpTunnelDescriptor descriptor = new HttpTunnelDescriptor(vhost, sessions);
        tunnelTokenDescriptors.put(sessions.tunnelToken(), descriptor);
        vhostDescriptors.put(vhost, descriptor);
        logger.info("Start Tunnel: {}", sessions.protoRequest());
        logger.trace("vhostDescriptors: {}", vhostDescriptors);
        logger.trace("tunnelTokenDescriptors: {}", tunnelTokenDescriptors);
    }

    /* package */
    synchronized void unregister(@Nullable String vhost) {
        if (vhost == null) {
            return;
        }
        final HttpTunnelDescriptor descriptor = vhostDescriptors.remove(vhost);
        if (descriptor != null) {
            tunnelTokenDescriptors.remove(descriptor.tunnelSessions().tunnelToken());
            descriptor.close();
            logger.info("Shutdown Tunnel: {}", descriptor.tunnelSessions().protoRequest());
        }
    }

    @Nullable
    /* package */ synchronized Channel getSessionChannel(long tunnelToken, long sessionToken) {
        HttpTunnelDescriptor descriptor = tunnelTokenDescriptors.get(tunnelToken);
        if (descriptor == null) {
            return null;
        }
        return descriptor.tunnelSessions().getSessionChannel(sessionToken);
    }

    @Nullable
    /* package */ synchronized HttpTunnelDescriptor getDescriptorByTunnelToken(long tunnelToken) {
        return tunnelTokenDescriptors.get(tunnelToken);
    }

    @Nullable
    /* package */ synchronized HttpTunnelDescriptor getDescriptorByVhost(@NotNull String vhost) {
        return vhostDescriptors.get(vhost);
    }

    /* package */
    synchronized void destroy() {
        tunnelTokenDescriptors.clear();
        vhostDescriptors.clear();
    }

}
