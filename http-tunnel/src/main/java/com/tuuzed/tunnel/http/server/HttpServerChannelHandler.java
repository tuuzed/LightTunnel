package com.tuuzed.tunnel.http.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import com.tuuzed.tunnel.http.util.HttpRequestUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

public class HttpServerChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerChannelHandler.class);
    private String vhost = "tunnel.lo";

    private final TunnelServerChannels tunnelServerChannels;
    private final HttpServerChannels httpServerChannels;

    public HttpServerChannelHandler(
            @NotNull TunnelServerChannels httpTunnelChannels,
            @NotNull HttpServerChannels httpServerChannels
    ) {
        this.tunnelServerChannels = httpTunnelChannels;
        this.httpServerChannels = httpServerChannels;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelInactive: {}", ctx);
        final String subDomain = ctx.channel().attr(TunnelAttributeKey.SUB_DOMAIN).get();
        if (subDomain != null) {
            final Channel nextChannel = tunnelServerChannels.getChannel(subDomain);
            if (nextChannel != null) {
                final Long tunnelToken = nextChannel.attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
                final Long sessionToken = ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).get();
                if (tunnelToken != null && sessionToken != null) {
                    Channel channel = httpServerChannels.removeChannel(tunnelToken, sessionToken);
                    if (channel != null) {
                        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            String host = ((HttpRequest) msg).headers().get(HttpHeaderNames.HOST);
            final String subDomain = host.substring(0, host.indexOf(vhost) - 1);
            ctx.channel().attr(TunnelAttributeKey.SUB_DOMAIN).set(subDomain);
            byte[] requestBytes = HttpRequestUtils.encodeHttpRequest((HttpRequest) msg);
            final Channel nextChannel = tunnelServerChannels.getChannel(subDomain);
            if (nextChannel != null) {
                final Long tunnelToken = nextChannel.attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
                final long sessionToken = nextChannel.attr(TunnelAttributeKey.SESSION_TOKEN_PRODUCER).get().incrementAndGet();

                httpServerChannels.putChannel(tunnelToken, sessionToken, ctx.channel());

                ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);
                nextChannel.writeAndFlush(
                        TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_TRANSFER)
                                .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
                                .setData(requestBytes)
                );
            } else {
                ctx.close();
            }
        } else if (msg instanceof HttpContent) {
            final String subDomain = ctx.channel().attr(TunnelAttributeKey.SUB_DOMAIN).get();
            if (subDomain != null) {
                final Channel nextChannel = tunnelServerChannels.getChannel(subDomain);
                if (nextChannel != null) {
                    final Long tunnelToken = nextChannel.attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
                    final Long sessionToken = ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).get();
                    if (tunnelToken != null && sessionToken != null) {
                        byte[] contentBytes = new byte[((HttpContent) msg).content().readableBytes()];
                        ((HttpContent) msg).content().readBytes(contentBytes);
                        nextChannel.writeAndFlush(
                                TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_TRANSFER)
                                        .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
                                        .setData(contentBytes)
                        );
                    }
                }
            }

        }
    }

}
