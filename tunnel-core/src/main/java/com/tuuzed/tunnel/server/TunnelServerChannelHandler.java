package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import com.tuuzed.tunnel.common.protocol.TunnelUri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * Tunnel服务数据通道处理器
 */
public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);

    @Nullable
    private OpenTunnelRequestInterceptor openTunnelRequestInterceptor;

    @NotNull
    public TunnelServerChannelHandler setOpenTunnelRequestInterceptor(OpenTunnelRequestInterceptor interceptor) {
        this.openTunnelRequestInterceptor = interceptor;
        return this;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        Long tunnelToken = getTunnelToken(ctx);
        if (tunnelToken != null) {
            UserTunnelManager.getInstance().closeUserTunnel(tunnelToken);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.info("Recv: {}", msg);
        switch (msg.getType()) {
            case MESSAGE_TYPE_HEARTBEAT_PING:
                handleHeartbeatPingMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                handleOpenTunnelRequestMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_LOCAL_TUNNEL_CONNECTED:
                handleLocalTunnelConnectedMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT:
                handleLocalTunnelDisconnectMessage(ctx, msg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatPingMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        ctx.channel().writeAndFlush(TunnelMessage.newInstance(MESSAGE_TYPE_HEARTBEAT_PONG));
    }

    /**
     * 处理建立隧道请求消息
     */
    @SuppressWarnings("Duplicates")
    private void handleOpenTunnelRequestMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final byte[] head = msg.getHead();
        final String uri = new String(head);
        try {
            TunnelUri tunnelUri = TunnelUri.create(uri);
            logger.info("tunnelUri: {}", tunnelUri);

            if (openTunnelRequestInterceptor != null) {
                openTunnelRequestInterceptor.proceed(tunnelUri);
            }

            final String localAddr = tunnelUri.host;
            final int localPort = tunnelUri.port;
            final int remotePort = Integer.parseInt(tunnelUri.queryMap.get("remotePort"));

            ctx.channel().attr(ATTR_URI).set(uri);
            ctx.channel().attr(ATTR_LOCAL_ADDR).set(localAddr);
            ctx.channel().attr(ATTR_LOCAL_PORT).set(localPort);
            ctx.channel().attr(ATTR_REMOTE_PORT).set(remotePort);

            long tunnelToken = UserTunnelManager.getInstance().openUserTunnel(remotePort, ctx.channel());
            ctx.channel().attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);
            ctx.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                            .setHead(Unpooled.copyLong(tunnelToken).array())
            );
        } catch (Exception e) {
            logger.error("openUserTunnel Error: {}", e.getMessage(), e);
            // 开启隧道异常，关闭连接
            ctx.close();
        }

    }

    /**
     * 处理数据透传消息
     * 数据流向: TunnelClient  ->  UserTunnelManager
     */
    @SuppressWarnings("Duplicates")
    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        long[] tunnelTokenAndSessionToken = getTunnelTokenAndSessionToken(msg);
        if (tunnelTokenAndSessionToken == null) {
            return;
        }
        final long tunnelToken = tunnelTokenAndSessionToken[0];
        final long sessionToken = tunnelTokenAndSessionToken[1];
        final UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByTunnelToken(tunnelToken);
        if (tunnel != null) {
            Channel userTunnelChannel = tunnel.getUserTunnelChannel(tunnelToken, sessionToken);
            if (userTunnelChannel != null) {
                userTunnelChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
            }
        }
    }

    /**
     * 处理本地隧道连接成功消息
     */
    @SuppressWarnings("Duplicates")
    private void handleLocalTunnelConnectedMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        long[] tunnelTokenAndSessionToken = getTunnelTokenAndSessionToken(msg);
        if (tunnelTokenAndSessionToken == null) {
            return;
        }
        final long tunnelToken = tunnelTokenAndSessionToken[0];
        final long sessionToken = tunnelTokenAndSessionToken[1];
        final UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByTunnelToken(tunnelToken);
        if (tunnel != null) {
            Channel userTunnelChannel = tunnel.getUserTunnelChannel(tunnelToken, sessionToken);
            if (userTunnelChannel != null) {
                userTunnelChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
            }
        }
    }

    /**
     * 处理本地隧道断开连接消息
     */
    @SuppressWarnings("Duplicates")
    private void handleLocalTunnelDisconnectMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        long[] tunnelTokenAndSessionToken = getTunnelTokenAndSessionToken(msg);
        if (tunnelTokenAndSessionToken == null) {
            return;
        }
        final long tunnelToken = tunnelTokenAndSessionToken[0];
        final long sessionToken = tunnelTokenAndSessionToken[1];
        UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByTunnelToken(tunnelToken);
        if (tunnel != null) {
            Channel userTunnelChannel = tunnel.getUserTunnelChannel(tunnelToken, sessionToken);
            if (userTunnelChannel != null) {
                // 解决 HTTP/1.x 数据传输问题
                userTunnelChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }


    // ====================== 工具方法 =========================== //

    @Nullable
    private static long[] getTunnelTokenAndSessionToken(TunnelMessage msg) {
        if (msg == null) {
            return null;
        }
        if (msg.getHead().length < 16) {
            return null;
        }
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        return new long[]{tunnelToken, sessionToken};
    }

    /**
     * 获取隧道令牌
     */
    @SuppressWarnings("Duplicates")
    @Nullable
    private static Long getTunnelToken(ChannelHandlerContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.channel().hasAttr(ATTR_TUNNEL_TOKEN)) {
            return ctx.channel().attr(ATTR_TUNNEL_TOKEN).get();
        }
        return null;
    }

}
