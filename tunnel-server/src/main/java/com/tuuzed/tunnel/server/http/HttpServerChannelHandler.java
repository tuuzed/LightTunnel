package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import com.tuuzed.tunnel.common.util.HttpRequestUtils;
import com.tuuzed.tunnel.server.internal.AttributeKeys;
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

    @NotNull
    private final HttpServer httpServer;

    public HttpServerChannelHandler(@NotNull HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String vhost = ctx.channel().attr(AttributeKeys.VHOST).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        if (vhost != null && sessionToken != null) {
            final HttpServer.Descriptor descriptor = httpServer.getDescriptorByVhost(vhost);
            if (descriptor != null) {
                final long tunnelToken = descriptor.tunnelSessions().tunnelToken();
                final Channel tunnelChannel = descriptor.tunnelSessions().tunnelChannel();
                tunnelChannel.writeAndFlush(
                    new ProtoMessage(
                        ProtoMessage.Type.REMOTE_DISCONNECT,
                        Unpooled.copyLong(tunnelToken, sessionToken).array(),
                        null
                    )
                );
            }
            ctx.channel().attr(AttributeKeys.VHOST).set(null);
            ctx.channel().attr(AttributeKeys.SESSION_TOKEN).set(null);
        }
        super.channelInactive(ctx);
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final String host = ((HttpRequest) msg).headers().get(HttpHeaderNames.HOST);
            final String vhost = host.split(":")[0];
            logger.trace("vhost: {}", vhost);
            ctx.channel().attr(AttributeKeys.VHOST).set(vhost);
            final HttpServer.Descriptor descriptor = httpServer.getDescriptorByVhost(vhost);
            logger.trace("descriptor: {}", descriptor);
            if (descriptor != null) {
                final long sessionToken = descriptor.tunnelSessions().putSessionChannel(ctx.channel());
                ctx.channel().attr(AttributeKeys.SESSION_TOKEN).set(sessionToken);

                final long tunnelToken = descriptor.tunnelSessions().tunnelToken();
                final byte[] requestBytes = HttpRequestUtils.httpRequest2Bytes((HttpRequest) msg);

                final Channel tunnelChannel = descriptor.tunnelSessions().tunnelChannel();
                tunnelChannel.writeAndFlush(
                    new ProtoMessage(ProtoMessage.Type.TRANSFER,
                        Unpooled.copyLong(tunnelToken, sessionToken).array(),
                        requestBytes
                    )
                );
            } else {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } else if (msg instanceof HttpContent) {
            final String vhost = ctx.channel().attr(AttributeKeys.VHOST).get();
            final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
            if (vhost != null && sessionToken != null) {
                final HttpServer.Descriptor descriptor = httpServer.getDescriptorByVhost(vhost);
                logger.trace("descriptor: {}", descriptor);
                if (descriptor != null) {
                    final long tunnelToken = descriptor.tunnelSessions().tunnelToken();
                    final Channel tunnelChannel = descriptor.tunnelSessions().tunnelChannel();
                    final byte[] contentBytes = new byte[((HttpContent) msg).content().readableBytes()];
                    ((HttpContent) msg).content().readBytes(contentBytes);
                    tunnelChannel.writeAndFlush(
                        new ProtoMessage(ProtoMessage.Type.TRANSFER,
                            Unpooled.copyLong(tunnelToken, sessionToken).array(),
                            contentBytes
                        )
                    );
                } else {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            } else {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
