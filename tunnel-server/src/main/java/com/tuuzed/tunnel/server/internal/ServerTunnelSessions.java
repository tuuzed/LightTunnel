package com.tuuzed.tunnel.server.internal;

import com.tuuzed.tunnel.proto.ProtoRequest;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTunnelSessions {

    private final long tunnelToken;
    @NotNull
    private final ProtoRequest protoRequest;
    @NotNull
    private final Channel tunnelChannel;
    @NotNull
    private final TokenProducer sessionTokenProducer;
    @NotNull
    private final Map<Long, Channel> cachedSessionChannels;

    public ServerTunnelSessions(
        long tunnelToken,
        @NotNull ProtoRequest protoRequest,
        @NotNull Channel tunnelChannel
    ) {
        this.tunnelToken = tunnelToken;
        this.protoRequest = protoRequest;
        this.tunnelChannel = tunnelChannel;
        this.sessionTokenProducer = new TokenProducer();
        this.cachedSessionChannels = new ConcurrentHashMap<>();
    }

    public long tunnelToken() {
        return tunnelToken;
    }

    @NotNull
    public ProtoRequest protoRequest() {
        return protoRequest;
    }

    @NotNull
    public Channel tunnelChannel() {
        return tunnelChannel;
    }

    public long putSessionChannel(@NotNull Channel channel) {
        final long sessionToken = sessionTokenProducer.nextToken();
        synchronized (cachedSessionChannels) {
            cachedSessionChannels.put(sessionToken, channel);
        }
        return sessionToken;
    }

    @Nullable
    public Channel getSessionChannel(long sessionToken) {
        synchronized (cachedSessionChannels) {
            return cachedSessionChannels.get(sessionToken);
        }
    }

    @Nullable
    public Channel removeSessionChannel(long sessionToken) {
        synchronized (cachedSessionChannels) {
            return cachedSessionChannels.remove(sessionToken);
        }
    }

    public void destroy() {
        synchronized (cachedSessionChannels) {
            shutdownAllSessionChannelUnsafe();
            cachedSessionChannels.clear();
        }
    }

    private void shutdownAllSessionChannelUnsafe() {
        final Collection<Channel> channels = cachedSessionChannels.values();
        for (Channel ch : channels) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
