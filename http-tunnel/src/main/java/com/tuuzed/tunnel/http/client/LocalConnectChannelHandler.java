package com.tuuzed.tunnel.http.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class LocalConnectChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(LocalConnectChannelHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final Channel nextChannel = ctx.channel().attr(TunnelAttributeKey.NEXT_CHANNEL).get();
        if (nextChannel != null) {
            // 本地连接断开
        }
        super.channelInactive(ctx);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final Channel nextChannel = ctx.channel().attr(TunnelAttributeKey.NEXT_CHANNEL).get();
        logger.debug("nextChannel: {} ", nextChannel);
        if (nextChannel != null) {
            logger.debug("nextChannel.isActive: {}", nextChannel.isActive());
            final Long tunnelToken = ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
            final Long sessionToken = ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).get();
            logger.debug("tunnelToken: {}, sessionToken: {}", tunnelToken, sessionToken);
            if (tunnelToken != null && sessionToken != null) {
                byte[] data = new byte[msg.readableBytes()];
                msg.readBytes(data);
                nextChannel.writeAndFlush(
                        TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_TRANSFER)
                                .setHead(Unpooled.copyLong(tunnelToken, sessionToken).array())
                                .setData(data)
                );
            }
        }
    }

}
