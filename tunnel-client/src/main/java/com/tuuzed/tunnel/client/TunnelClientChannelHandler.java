package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;


/**
 * Tunnel客户端数据通道处理器
 */
@SuppressWarnings("Duplicates")
class TunnelClientChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);
    @NotNull
    private TunnelClientChannelListener tunnelClientChannelListener;
    @NotNull
    private final LocalConnectManager localConnectManager;

    public TunnelClientChannelHandler(@NotNull LocalConnectManager manager, @NotNull TunnelClientChannelListener listener) {
        this.localConnectManager = manager;
        this.tunnelClientChannelListener = listener;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        final Long tunnelToken = ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).get();
        if (tunnelToken != null && sessionToken != null) {
            localConnectManager.removeLocalConnectChannel(tunnelToken, sessionToken);
        }
        super.channelInactive(ctx);
        tunnelClientChannelListener.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.trace("exceptionCaught: ", cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.trace("Recv : {}", msg);
        switch (msg.getType()) {
            case TunnelMessage.MESSAGE_TYPE_HEARTBEAT_PING:
                handleHeartbeatPingMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                handleOpenTunnelResponseMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_USER_TUNNEL_CONNECTED:
                handleUserChannelConnectedMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_USER_TUNNEL_DISCONNECT:
                handleUserChannelDisconnect(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理心跳数据
     */
    private void handleHeartbeatPingMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        ctx.channel().writeAndFlush(TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_HEARTBEAT_PONG));
    }

    /**
     * 处理建立隧道请求消息
     */
    private void handleOpenTunnelResponseMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final byte status = head.readByte();
        if (status == TunnelMessage.OPEN_TUNNEL_RESPONSE_SUCCESS) {
            // 开启隧道成功
            final long tunnelToken = head.readLong();
            final OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(msg.getData());
            ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).set(openTunnelRequest);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).set(null);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).set(null);
            logger.debug("Opened Tunnel: {}", openTunnelRequest);
            tunnelClientChannelListener.tunnelConnected(ctx);
        } else if (status == TunnelMessage.OPEN_TUNNEL_RESPONSE_FAILURE) {
            // 开启隧道失败
            final String openTunnelMessage = new String(msg.getData(), StandardCharsets.UTF_8);
            ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(null);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).set(null);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).set(true);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).set(openTunnelMessage);
            ctx.channel().close();
            logger.debug("Open Tunnel Error: {}", openTunnelMessage);
        } else {
            // 开启隧道失败
            final String openTunnelMessage = new String(msg.getData(), StandardCharsets.UTF_8);
            ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(null);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).set(null);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).set(true);
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).set("Protocol error");
            ctx.channel().close();
            logger.debug("Open Tunnel Error: {}", openTunnelMessage);
        }
        head.release();
    }

    /**
     * 处理连接本地隧道消息
     */
    private void handleUserChannelConnectedMessage(final ChannelHandlerContext ctx, final TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);

        final OpenTunnelRequest openTunnelRequest = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).get();
        final String localAddr = openTunnelRequest.localAddr;
        final int localPort = openTunnelRequest.localPort;

        localConnectManager.getLocalConnectChannel(localAddr, localPort, tunnelToken, sessionToken,
                ctx.channel(),
                new LocalConnectManager.GetLocalTunnelChannelCallback() {
                    @Override
                    public void success(@NotNull Channel localTunnelChannel) {
                    }

                    @Override
                    public void error(Throwable cause) {
                    }
                });
    }

    /**
     * 处理数据透传消息
     */
    private void handleTransferMessage(final ChannelHandlerContext ctx, final TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);
        OpenTunnelRequest openTunnelRequest = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).get();
        if (openTunnelRequest != null) {
            localConnectManager.getLocalConnectChannel(
                    openTunnelRequest.localAddr, openTunnelRequest.localPort,
                    tunnelToken, sessionToken,
                    ctx.channel(),
                    new LocalConnectManager.GetLocalTunnelChannelCallback() {
                        @Override
                        public void success(@NotNull Channel localTunnelChannel) {
                            localTunnelChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
                        }

                        @Override
                        public void error(Throwable cause) {
                        }
                    }
            );
        }
    }

    /**
     * 处理用户隧道断开消息
     */
    private void handleUserChannelDisconnect(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        localConnectManager.removeLocalConnectChannel(tunnelToken, sessionToken);
    }

}
