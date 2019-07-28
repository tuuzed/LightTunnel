package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.internal.AttributeKeys;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("Duplicates")
public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<ProtoMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);
    @NotNull
    private final TcpServer tcpServer;
    @NotNull
    private final HttpServer httpServer;
    @NotNull
    private final ProtoRequest.Interceptor protoRequestInterceptor;
    @NotNull
    private final TokenProducer tunnelTokenProducer;


    public TunnelServerChannelHandler(
        @NotNull TcpServer tcpServer,
        @NotNull HttpServer httpServer,
        @NotNull ProtoRequest.Interceptor protoRequestInterceptor,
        @NotNull TokenProducer tunnelTokenProducer
    ) {
        this.tcpServer = tcpServer;
        this.httpServer = httpServer;
        this.protoRequestInterceptor = protoRequestInterceptor;
        this.tunnelTokenProducer = tunnelTokenProducer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final ServerTunnelSessions tunnelSessions = ctx.channel().attr(AttributeKeys.SERVER_TUNNEL_SESSIONS).get();
        if (tunnelSessions != null) {
            final long tunnelToken = tunnelSessions.tunnelToken();
            if (tunnelSessions.isHttp()) {
                httpServer.unregister(tunnelToken);
            } else if (tunnelSessions.isTcp()) {
                tcpServer.shutdownTunnel(tunnelToken);
            }
        }
        ctx.channel().attr(AttributeKeys.SERVER_TUNNEL_SESSIONS).set(null);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.trace("exceptionCaught: ", cause);
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        switch (msg.getType()) {
            case HEARTBEAT_PING:
                handlePingMessage(ctx, msg);
                break;
            case REQUEST:
                handleRequestMessage(ctx, msg);
                break;
            case TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case LOCAL_CONNECTED:
                handleLocalConnectedMessage(ctx, msg);
                break;
            case LOCAL_DISCONNECT:
                handleLocalDisconnectMessage(ctx, msg);
                break;
            default:
                break;
        }
    }

    private void handlePingMessage(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        ctx.writeAndFlush(new ProtoMessage(ProtoMessage.Type.HEARTBEAT_PONG, null, null));
    }

    private void handleRequestMessage(ChannelHandlerContext ctx, ProtoMessage msg) throws Exception {
        ProtoRequest protoRequest = ProtoRequest.fromBytes(msg.getHead());
        switch (protoRequest.proto()) {
            case TCP:
                try {
                    protoRequest = protoRequestInterceptor.proceed(protoRequest);
                    final long tunnelToken = tunnelTokenProducer.nextToken();
                    final ServerTunnelSessions tunnelSessions = new ServerTunnelSessions(tunnelToken, protoRequest, ctx.channel());

                    ctx.channel().attr(AttributeKeys.SERVER_TUNNEL_SESSIONS).set(tunnelSessions);

                    tcpServer.startTunnel(null, protoRequest.remotePort(), tunnelSessions);

                    final ByteBuf head = Unpooled.buffer(9);
                    head.writeBoolean(true);
                    head.writeLong(tunnelToken);
                    ctx.channel().writeAndFlush(
                        new ProtoMessage(
                            ProtoMessage.Type.RESPONSE,
                            head.array(),
                            protoRequest.toBytes()
                        )
                    );
                    head.release();

                } catch (ProtoException e) {
                    final ByteBuf head = Unpooled.copyBoolean(false);
                    ctx.channel().writeAndFlush(
                        new ProtoMessage(
                            ProtoMessage.Type.RESPONSE,
                            head.array(),
                            e.getMessage().getBytes(StandardCharsets.UTF_8)
                        )
                    ).addListener(ChannelFutureListener.CLOSE);
                    head.release();
                }
                break;
            case HTTP:
                try {
                    protoRequest = protoRequestInterceptor.proceed(protoRequest);
                    final long tunnelToken = tunnelTokenProducer.nextToken();
                    final ServerTunnelSessions tunnelSessions = new ServerTunnelSessions(tunnelToken, protoRequest, ctx.channel());

                    ctx.channel().attr(AttributeKeys.SERVER_TUNNEL_SESSIONS).set(tunnelSessions);

                    httpServer.register(protoRequest.vhost(), tunnelSessions);

                    final ByteBuf head = Unpooled.buffer(9);
                    head.writeBoolean(true);
                    head.writeLong(tunnelToken);
                    ctx.channel().writeAndFlush(
                        new ProtoMessage(
                            ProtoMessage.Type.RESPONSE,
                            head.array(),
                            protoRequest.toBytes()
                        )
                    );
                    head.release();

                } catch (ProtoException e) {
                    final ByteBuf head = Unpooled.copyBoolean(false);
                    ctx.channel().writeAndFlush(
                        new ProtoMessage(
                            ProtoMessage.Type.RESPONSE,
                            head.array(),
                            e.getMessage().getBytes(StandardCharsets.UTF_8)
                        )
                    ).addListener(ChannelFutureListener.CLOSE);
                    head.release();
                }
                break;
            default:
                final ByteBuf head = Unpooled.copyBoolean(false);
                ctx.channel().writeAndFlush(
                    new ProtoMessage(
                        ProtoMessage.Type.RESPONSE,
                        head.array(),
                        "协议错误".getBytes(StandardCharsets.UTF_8)
                    )
                ).addListener(ChannelFutureListener.CLOSE);
                head.release();
                break;
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProtoMessage msg) {
        final ServerTunnelSessions tunnelSessions = ctx.channel().attr(AttributeKeys.SERVER_TUNNEL_SESSIONS).get();
        if (tunnelSessions != null) {
            final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
            final long tunnelToken = head.readLong();
            final long sessionToken = head.readLong();
            head.release();
            if (tunnelSessions.isTcp()) {
                final TcpServer.Descriptor descriptor = tcpServer.getDescriptorTunnelToken(tunnelToken);
                if (descriptor != null) {
                    final Channel sessionChannel = descriptor.tunnelSessions().getSessionChannel(sessionToken);
                    if (sessionChannel != null) {
                        sessionChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
                    }
                }
            } else if (tunnelSessions.isHttp()) {
                final HttpServer.Descriptor descriptor = httpServer.getDescriptorTunnelToken(tunnelToken);
                if (descriptor != null) {
                    final Channel sessionChannel = descriptor.tunnelSessions().getSessionChannel(sessionToken);
                    if (sessionChannel != null) {
                        sessionChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
                    }
                }
            }
        }
    }

    private void handleLocalConnectedMessage(ChannelHandlerContext ctx, ProtoMessage msg) {
        // 无需操作
    }

    private void handleLocalDisconnectMessage(ChannelHandlerContext ctx, ProtoMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        final TcpServer.Descriptor descriptor = tcpServer.getDescriptorTunnelToken(tunnelToken);
        if (descriptor != null) {
            final Channel sessionChannel = descriptor.tunnelSessions().getSessionChannel(sessionToken);
            if (sessionChannel != null) {
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }


}
