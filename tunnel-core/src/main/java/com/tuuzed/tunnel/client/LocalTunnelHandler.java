package com.tuuzed.tunnel.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 本地连接数据通道处理器
 */
public class LocalTunnelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(LocalTunnelHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int length = msg.readableBytes();
        byte[] data = new byte[length];
        msg.readBytes(data);
        Channel nextChannel = ctx.channel().attr(ATTR_NEXT_CHANNEL).get();
        logger.info("nextChannel: {}", nextChannel);
        nextChannel.writeAndFlush(
                TunnelMessage.newInstance(MESSAGE_TYPE_TRANSFER)
                        .setHead(nextChannel.attr(ATTR_MAPPING).get().getBytes())
                        .setData(data)
        );

    }

}