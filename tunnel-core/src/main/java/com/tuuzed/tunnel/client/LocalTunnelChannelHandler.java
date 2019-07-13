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

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 本地连接数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class LocalTunnelChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(LocalTunnelChannelHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelInactive: {}", ctx);
        final Long tunnelToken = ctx.channel().attr(ATTR_TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        if (tunnelToken != null && sessionToken != null) {
            LocalTunnelChannelManager.getInstance().removeLocalTunnelChannel(tunnelToken, sessionToken);
        }
        final Channel tunnelClientChannel = ctx.channel().attr(ATTR_NEXT_CHANNEL).get();
        if (tunnelClientChannel != null) {
            tunnelClientChannel.writeAndFlush(
                    TunnelMessage.newInstance(MESSAGE_TYPE_LOCAL_TUNNEL_DISCONNECT)
                            .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
            );
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
        final Long tunnelToken = ctx.channel().attr(ATTR_TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(ATTR_SESSION_TOKEN).get();
        final Channel tunnelClientChannel = ctx.channel().attr(ATTR_NEXT_CHANNEL).get();
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
        final Channel tunnelClientChannel = ctx.channel().attr(ATTR_NEXT_CHANNEL).get();
        if (tunnelClientChannel != null) {
            tunnelClientChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }
}