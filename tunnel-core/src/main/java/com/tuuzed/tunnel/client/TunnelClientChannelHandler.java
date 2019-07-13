package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * Tunnel客户端数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class TunnelClientChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);

    @Nullable
    private TunnelClientChannelListener tunnelClientChannelListener;

    @NotNull
    public TunnelClientChannelHandler setTunnelClientChannelListener(TunnelClientChannelListener listener) {
        this.tunnelClientChannelListener = listener;
        return this;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        final Long tunnelToken = ctx.channel().attr(ATTR_TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        if (tunnelToken != null && sessionToken != null) {
            LocalTunnelChannelManager.getInstance().removeLocalTunnelChannel(tunnelToken, sessionToken);
        }
        super.channelInactive(ctx);
        if (tunnelClientChannelListener != null) {
            tunnelClientChannelListener.channelInactive(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.info("Recv : {}", msg);
        switch (msg.getType()) {
            case MESSAGE_TYPE_HEARTBEAT_PING:
                handleHeartbeatPingMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                handleOpenTunnelResponseMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_CONNECT_LOCAL_TUNNEL:
                handleConnectLocalTunnelMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_USER_TUNNEL_DISCONNECT:
                handleUserChannelDisconnect(ctx, msg);
                break;
            default:
                break;
        }
    }


    /**
     * 处理心跳数据
     */
    private void handleHeartbeatPingMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        ctx.channel().writeAndFlush(TunnelMessage.newInstance(MESSAGE_TYPE_HEARTBEAT_PONG));
    }


    /**
     * 处理建立隧道请求消息
     */
    private void handleOpenTunnelResponseMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        ctx.channel().attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);

        OpenTunnelRequest openTunnelRequest = ctx.channel().attr(ATTR_OPEN_TUNNEL_REQUEST).get();
        logger.info("Opened Tunnel: {}", openTunnelRequest);
    }

    /**
     * 处理连接本地隧道消息
     */
    private void handleConnectLocalTunnelMessage(final ChannelHandlerContext ctx, final TunnelMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        ctx.channel().attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(ATTR_SESSION_TOKEN).set(sessionToken);

        final OpenTunnelRequest openTunnelRequest = ctx.channel().attr(ATTR_OPEN_TUNNEL_REQUEST).get();
        final String localAddr = openTunnelRequest.localAddr;
        final int localPort = openTunnelRequest.localPort;

        LocalTunnelChannelManager.getInstance().getLocalTunnelChannel(localAddr, localPort, tunnelToken, sessionToken,
                ctx.channel(),
                new LocalTunnelChannelManager.GetLocalTunnelChannelCallback() {
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
     * 数据流向: UserTunnelManager -> LocalTunnelChannelManager
     */
    private void handleTransferMessage(final ChannelHandlerContext ctx, final TunnelMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();

        ctx.channel().attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(ATTR_SESSION_TOKEN).set(sessionToken);
        OpenTunnelRequest openTunnelRequest = ctx.channel().attr(ATTR_OPEN_TUNNEL_REQUEST).get();
        if (openTunnelRequest != null) {
            LocalTunnelChannelManager.getInstance().getLocalTunnelChannel(
                    openTunnelRequest.localAddr, openTunnelRequest.localPort,
                    tunnelToken, sessionToken,
                    ctx.channel(),
                    new LocalTunnelChannelManager.GetLocalTunnelChannelCallback() {
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

    private void handleUserChannelDisconnect(ChannelHandlerContext ctx, TunnelMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        LocalTunnelChannelManager.getInstance().removeLocalTunnelChannel(tunnelToken, sessionToken);
    }
}
