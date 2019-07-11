package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 隧道数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class UserTunnelChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(UserTunnelChannelHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UserTunnel tunnel = getUserTunnel(ctx);
        if (tunnel == null) {
            return;
        }
        final Channel serverChannel = tunnel.serverChannel();
        final long tunnelToken = serverChannel.attr(ATTR_TUNNEL_TOKEN).get();
        final long sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        tunnel.removeUserTunnelChannel(tunnelToken, sessionToken);
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        UserTunnel tunnel = getUserTunnel(ctx);
        if (tunnel == null) {
            return;
        }
        final Channel serverChannel = tunnel.serverChannel();
        final long tunnelToken = serverChannel.attr(ATTR_TUNNEL_TOKEN).get();
        final long sessionToken;
        if (ctx.channel().hasAttr(ATTR_SESSION_TOKEN)) {
            sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        } else {
            sessionToken = tunnel.generateSessionToken();
            ctx.channel().attr(ATTR_SESSION_TOKEN).set(sessionToken);
        }
        tunnel.putUserTunnelChannel(tunnelToken, sessionToken, ctx.channel());
        serverChannel.writeAndFlush(
                TunnelMessage.newInstance(MESSAGE_TYPE_CONNECT_LOCAL_TUNNEL)
                        .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
        );
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int length = msg.readableBytes();
        byte[] data = new byte[length];
        msg.readBytes(data);
        // 根据入站端口获取用户隧道,如果用户隧道不存在则直接关闭连接
        UserTunnel tunnel = getUserTunnel(ctx);
        if (tunnel == null) {
            return;
        }
        final Channel serverChannel = tunnel.serverChannel();
        final long tunnelToken = serverChannel.attr(ATTR_TUNNEL_TOKEN).get();
        final long sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        // 将数据转发至TunnelClient
        serverChannel.writeAndFlush(
                TunnelMessage.newInstance(MESSAGE_TYPE_TRANSFER)
                        .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
                        .setData(data)
        );
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        int inboundPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByBindPort(inboundPort);
        if (tunnel == null) {
            return;
        }
        Channel serverChannel = tunnel.serverChannel();
        serverChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Nullable
    private UserTunnel getUserTunnel(ChannelHandlerContext ctx) {
        int inboundPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        UserTunnel tunnel = UserTunnelManager.getInstance().getUserTunnelByBindPort(inboundPort);
        if (tunnel == null) {
            ctx.close();
            return null;
        }
        return tunnel;
    }
}
