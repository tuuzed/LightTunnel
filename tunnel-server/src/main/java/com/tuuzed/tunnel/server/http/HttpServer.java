package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    @NotNull
    private final ServerBootstrap serverBootstrap;
    @NotNull
    private final Map<Long, HttpTunnelDescriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, HttpTunnelDescriptor> vhostDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Object descriptorsLock = new Object();

    public HttpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup,
        @NotNull final HttpRequestInterceptor interceptor,
        @Nullable final SslContext sslContext
    ) {
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    if (sslContext != null) { // 启用SSL
                        ch.pipeline().addFirst(new SslHandler(sslContext.newEngine(ch.alloc())));
                    }
                    ch.pipeline()
                        .addLast(new HttpRequestDecoder())
                        .addLast(new HttpServerChannelHandler(HttpServer.this, interceptor))
                    ;
                }
            });
    }


    public boolean isRegistered(@NotNull String vhost) {
        synchronized (descriptorsLock) {
            return vhostDescriptors.containsKey(vhost);
        }
    }

    public void register(@NotNull String vhost, @NotNull ServerTunnelSessions tunnelSessions) throws Exception {
        if (isRegistered(vhost)) {
            throw new ProtoException("vhost(" + vhost + ") already used");
        }
        final HttpTunnelDescriptor descriptor = new HttpTunnelDescriptor(vhost, tunnelSessions);
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.put(tunnelSessions.tunnelToken(), descriptor);
            vhostDescriptors.put(vhost, descriptor);
        }
        logger.info("Start Tunnel: {}", tunnelSessions.protoRequest());
        logger.trace("vhostDescriptors: {}", vhostDescriptors);
        logger.trace("tunnelTokenDescriptors: {}", tunnelTokenDescriptors);

    }


    public void unregister(@Nullable String vhost) {
        if (vhost == null) {
            return;
        }
        synchronized (descriptorsLock) {
            final HttpTunnelDescriptor descriptor = vhostDescriptors.remove(vhost);
            if (descriptor != null) {
                tunnelTokenDescriptors.remove(descriptor.tunnelSessions().tunnelToken());
                descriptor.close();
                logger.info("Shutdown Tunnel: {}", descriptor.tunnelSessions().protoRequest());
            }
        }
    }

    @Nullable
    public Channel getSessionChannel(long tunnelToken, long sessionToken) {
        HttpTunnelDescriptor descriptor = getDescriptorByTunnelToken(tunnelToken);
        if (descriptor == null) {
            return null;
        }
        return descriptor.tunnelSessions().getSessionChannel(sessionToken);
    }

    @Nullable
    public HttpTunnelDescriptor getDescriptorByTunnelToken(long tunnelToken) {
        synchronized (descriptorsLock) {
            return tunnelTokenDescriptors.get(tunnelToken);
        }
    }

    @Nullable
    public HttpTunnelDescriptor getDescriptorByVhost(@NotNull String vhost) {
        synchronized (descriptorsLock) {
            return vhostDescriptors.get(vhost);
        }
    }

    public void serve(@Nullable String bindAddr, int bindPort) throws Exception {
        if (bindAddr != null) {
            serverBootstrap.bind(bindAddr, bindPort).get();
        } else {
            serverBootstrap.bind(bindPort).get();
        }
    }

    public void destroy() {
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.clear();
            vhostDescriptors.clear();
        }
    }


}
