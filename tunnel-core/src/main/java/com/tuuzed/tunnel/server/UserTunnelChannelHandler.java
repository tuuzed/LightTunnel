package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 隧道数据通道处理器
 */
public class UserTunnelChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(UserTunnelChannelHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int length = msg.readableBytes();
        byte[] data = new byte[length];
        msg.readBytes(data);
        int inboundPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        UserTunnel tunnel = UserTunnelManager.getInstance().getTunnel(inboundPort);
        if (tunnel == null) {
            ctx.close();
            return;

        }
        tunnel.serverChannel().attr(ATTR_NEXT_CHANNEL).set(ctx.channel());
        Channel serverChannel = tunnel.serverChannel();
        logger.info("inboundPort: {}, serverChannel: {}", inboundPort, serverChannel);
        serverChannel.writeAndFlush(
                TunnelMessage.newInstance(MESSAGE_TYPE_TRANSFER)
                        .setHead(serverChannel.attr(ATTR_MAPPING).get().getBytes())
                        .setData(data)
        );
    }

}
