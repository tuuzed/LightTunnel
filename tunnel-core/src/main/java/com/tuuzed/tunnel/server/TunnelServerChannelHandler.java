package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.Interceptor;
import com.tuuzed.tunnel.common.TunnelProtocolException;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.Nullable;

import java.net.BindException;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * Tunnel服务数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);

    @Nullable
    private final Interceptor<OpenTunnelRequest> openTunnelRequestInterceptor;

    public TunnelServerChannelHandler(@Nullable Interceptor<OpenTunnelRequest> interceptor) {
        this.openTunnelRequestInterceptor = interceptor;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        final Long tunnelToken = ctx.channel().attr(ATTR_TUNNEL_TOKEN).get();
        if (tunnelToken != null) {
            UserTunnelManager.getInstance().closeUserTunnel(tunnelToken);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("exceptionCaught: ", cause);
        ctx.close();
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
    private void handleOpenTunnelRequestMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        try {
            OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(msg.getHead());
            logger.info("openTunnelRequest: {}", openTunnelRequest);
            if (openTunnelRequestInterceptor != null) {
                openTunnelRequest = openTunnelRequestInterceptor.proceed(openTunnelRequest);
            }
            ctx.channel().attr(ATTR_OPEN_TUNNEL_REQUEST).set(openTunnelRequest);
            final long tunnelToken = UserTunnelManager.getInstance()
                    .openUserTunnel(openTunnelRequest.remotePort, ctx.channel());
            ctx.channel().attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);
            ctx.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                            .setHead(Unpooled.copyLong(tunnelToken).array())
                            .setData(openTunnelRequest.toBytes())
            );
        } catch (TunnelProtocolException e) {
            ctx.channel().writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_OPEN_TUNNEL_ERROR)
                            .setHead(e.getMessage().getBytes())
            ).addListener(ChannelFutureListener.CLOSE);
        } catch (BindException e) {
            ctx.channel().writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_OPEN_TUNNEL_ERROR)
                            .setHead("Bind Port Error".getBytes())
            ).addListener(ChannelFutureListener.CLOSE);
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

    /**
     * 处理本地隧道断开连接消息
     */
    private void handleLocalTunnelDisconnectMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
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

}
