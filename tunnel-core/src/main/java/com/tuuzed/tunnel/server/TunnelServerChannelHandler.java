package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.Nullable;

import java.net.BindException;
import java.nio.charset.StandardCharsets;


/**
 * Tunnel服务数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);

    @Nullable
    private final OpenTunnelRequestInterceptor openTunnelRequestInterceptor;

    public TunnelServerChannelHandler(@Nullable OpenTunnelRequestInterceptor interceptor) {
        this.openTunnelRequestInterceptor = interceptor;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        final Long tunnelToken = ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
        logger.trace("channelInactive: {}, {}", tunnelToken, ctx);
        if (tunnelToken != null) {
            UserTunnelManager.getInstance().closeUserTunnel(tunnelToken);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.trace("exceptionCaught: ", cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.trace("Recv: {}", msg);
        switch (msg.getType()) {
            case TunnelMessage.MESSAGE_TYPE_HEARTBEAT_PING:
                handleHeartbeatPingMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                handleOpenTunnelRequestMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_LOCAL_CONNECT_CONNECTED:
                handleLocalConnectConnectedMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_LOCAL_CONNECT_DISCONNECT:
                handleLocalConnectDisconnectMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            default:
                break;
        }
    }


    /**
     * 处理心跳消息
     */
    private void handleHeartbeatPingMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        ctx.channel().writeAndFlush(TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_HEARTBEAT_PONG));
    }

    /**
     * 处理建立隧道请求消息
     */
    private void handleOpenTunnelRequestMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        try {
            OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(msg.getHead());
            logger.trace("openTunnelRequest: {}", openTunnelRequest);
            if (openTunnelRequestInterceptor != null) {
                openTunnelRequest = openTunnelRequestInterceptor.proceed(openTunnelRequest);
            }
            ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).set(openTunnelRequest);
            final long tunnelToken = UserTunnelManager.getInstance()
                    .openUserTunnel(openTunnelRequest.remotePort, ctx.channel());
            ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
            final ByteBuf head = Unpooled.buffer(9);
            head.writeByte(1);
            head.writeLong(tunnelToken);
            ctx.writeAndFlush(
                    TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                            .setHead(head.array())
                            .setData(openTunnelRequest.toBytes())
            );
        } catch (TunnelProtocolException e) {
            ctx.writeAndFlush(
                    TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                            .setHead(new byte[]{0})
                            .setData(e.getMessage().getBytes(StandardCharsets.UTF_8))
            ).addListener(ChannelFutureListener.CLOSE);


        } catch (BindException e) {
            ctx.channel().writeAndFlush(
                    TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                            .setHead(new byte[]{0})
                            .setData("Bind Port Error".getBytes(StandardCharsets.UTF_8))
            ).addListener(ChannelFutureListener.CLOSE);
        }
    }


    private void handleLocalConnectConnectedMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        // pass
    }

    /**
     * 处理本地隧道断开连接消息
     */
    private void handleLocalConnectDisconnectMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByTunnelToken(tunnelToken);
        if (tunnel != null) {
            Channel userTunnelChannel = tunnel.getUserTunnelChannel(tunnelToken, sessionToken);
            if (userTunnelChannel != null) {
                // 解决 HTTP/1.x 数据传输问题
                userTunnelChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 处理数据透传消息
     */
    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        final UserTunnel userTunnel = UserTunnelManager.getInstance().getUserTunnelByTunnelToken(tunnelToken);
        if (userTunnel != null) {
            Channel userTunnelChannel = userTunnel.getUserTunnelChannel(tunnelToken, sessionToken);
            if (userTunnelChannel != null) {
                userTunnelChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
            }
        }
    }
}
