package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.HttpUtils;
import com.tuuzed.tunnel.server.internal.AttributeKeys;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

@SuppressWarnings("Duplicates")
public class HttpServerChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerChannelHandler.class);

    @NotNull
    private final HttpServer httpServer;
    @NotNull
    private final HttpRequestInterceptor httpRequestInterceptor;

    public HttpServerChannelHandler(
        @NotNull HttpServer httpServer,
        @NotNull HttpRequestInterceptor interceptor
    ) {
        this.httpServer = httpServer;
        this.httpRequestInterceptor = interceptor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.trace("channelActive: {}", ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelInactive: {}", ctx);
        final String vhost = ctx.channel().attr(AttributeKeys.VHOST).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        if (vhost != null && sessionToken != null) {
            final HttpTunnelDescriptor descriptor = httpServer.getDescriptorByVhost(vhost);
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
        logger.trace("exceptionCaught: {}", ctx, cause);
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            channelReadHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof HttpContent) {
            channelReadHttpContent(ctx, (HttpContent) msg);
        }
    }

    /**
     * 处理读取到的HttpRequest类型的消息
     */
    private void channelReadHttpRequest(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        final String vhost = HttpUtils.getVhost(msg);
        if (vhost == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        ctx.channel().attr(AttributeKeys.VHOST).set(vhost);
        final HttpTunnelDescriptor descriptor = httpServer.getDescriptorByVhost(vhost);
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            ctx.channel().attr(AttributeKeys.PASS).set(false);
            return;
        }
        ctx.channel().attr(AttributeKeys.PASS).set(true);
        final ProtoRequest protoRequest = descriptor.tunnelSessions().protoRequest();
        final SocketAddress localAddress = ctx.channel().localAddress();
        final SocketAddress remoteAddress = ctx.channel().remoteAddress();

        final HttpResponse httpResponse = httpRequestInterceptor.handleHttpRequest(
            localAddress, remoteAddress,
            protoRequest, msg
        );
        if (httpResponse != null) {
            final ByteBuf byteBuf = HttpUtils.httpResponseToBytes(httpResponse);
            ctx.channel().writeAndFlush(byteBuf);
            return;
        }
        final long sessionToken = descriptor.tunnelSessions().putSessionChannel(ctx.channel());
        ctx.channel().attr(AttributeKeys.SESSION_TOKEN).set(sessionToken);
        final long tunnelToken = descriptor.tunnelSessions().tunnelToken();
        final byte[] requestBytes = HttpUtils.httpRequestToBytes(msg);
        descriptor.tunnelSessions().tunnelChannel().writeAndFlush(
            new ProtoMessage(ProtoMessage.Type.TRANSFER,
                Unpooled.copyLong(tunnelToken, sessionToken).array(),
                requestBytes
            )
        );
    }

    /**
     * 处理读取到的HttpContent类型的消息
     */
    private void channelReadHttpContent(ChannelHandlerContext ctx, HttpContent msg) throws Exception {
        final Boolean pass = ctx.channel().attr(AttributeKeys.PASS).get();
        if (!(pass != null && pass)) { // 如果没有放行
            return;
        }
        final String vhost = ctx.channel().attr(AttributeKeys.VHOST).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        if (vhost == null || sessionToken == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        final HttpTunnelDescriptor descriptor = httpServer.getDescriptorByVhost(vhost);
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        final long tunnelToken = descriptor.tunnelSessions().tunnelToken();
        final byte[] contentBytes = new byte[msg.content().readableBytes()];
        msg.content().readBytes(contentBytes);

        descriptor.tunnelSessions().tunnelChannel().writeAndFlush(
            new ProtoMessage(ProtoMessage.Type.TRANSFER,
                Unpooled.copyLong(tunnelToken, sessionToken).array(),
                contentBytes
            )
        );
    }

}
