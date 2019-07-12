package com.tuuzed.tunnel.client;

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

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 本地连接数据通道处理器
 */
public class LocalTunnelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(LocalTunnelHandler.class);


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final Long tunnelToken = getTunnelToken(ctx);
        final Long sessionToken = getSessionToken(ctx);
        final Channel tunnelClientChannel = getNextChannel(ctx);
        if (tunnelToken != null && sessionToken != null && tunnelClientChannel != null) {
            tunnelClientChannel.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_LOCAL_TUNNEL_CONNECTED)
                            .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
            );
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final Long tunnelToken = getTunnelToken(ctx);
        final Long sessionToken = getSessionToken(ctx);
        final Channel tunnelClientChannel = getNextChannel(ctx);
        if (tunnelToken != null && sessionToken != null && tunnelClientChannel != null) {
            tunnelClientChannel.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT)
                            .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
            );
            LocalTunnel.getInstance().removeLocalTunnelChannel(tunnelToken, sessionToken);

        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final int length = msg.readableBytes();
        final byte[] data = new byte[length];
        msg.readBytes(data);
        final Long tunnelToken = getTunnelToken(ctx);
        final Long sessionToken = getSessionToken(ctx);
        final Channel tunnelClientChannel = getNextChannel(ctx);
        if (tunnelToken != null && sessionToken != null && tunnelClientChannel != null) {
            tunnelClientChannel.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_TRANSFER)
                            .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
                            .setData(data)
            );
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        final Channel tunnelClientChannel = getNextChannel(ctx);
        if (tunnelClientChannel != null) {
            tunnelClientChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }

    @SuppressWarnings("Duplicates")
    @Nullable
    private static Channel getNextChannel(ChannelHandlerContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.channel().hasAttr(ATTR_NEXT_CHANNEL)) {
            return ctx.channel().attr(ATTR_NEXT_CHANNEL).get();
        }
        return null;
    }

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

    @SuppressWarnings("Duplicates")
    @Nullable
    private static Long getSessionToken(ChannelHandlerContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.channel().hasAttr(ATTR_SESSION_TOKEN)) {
            return ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        }
        return null;
    }

}