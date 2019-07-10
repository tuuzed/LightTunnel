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

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * 隧道数据通道处理器
 */
public class UserTunnelChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(UserTunnelChannelHandler.class);

    private static final AtomicLong sessionTokenGenerator = new AtomicLong();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int length = msg.readableBytes();
        byte[] data = new byte[length];
        msg.readBytes(data);
        // 根据入站端口获取用户隧道,如果用户隧道不存在则直接关闭连接
        int inboundPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        UserTunnel tunnel = UserTunnel.getManager().getUserTunnelByBindPort(inboundPort);
        if (tunnel == null) {
            ctx.close();
            return;
        }
        Channel serverChannel = tunnel.serverChannel();
        long tunnelToken = serverChannel.attr(ATTR_TUNNEL_TOKNE).get();
        long sessionToken;
        if (ctx.channel().hasAttr(ATTR_SESSION_TOKNE)) {
            sessionToken = ctx.channel().attr(ATTR_SESSION_TOKNE).get();
        } else {
            sessionToken = sessionTokenGenerator.incrementAndGet();
            ctx.channel().attr(ATTR_SESSION_TOKNE).set(sessionToken);
        }
        tunnel.putUserTunnelChannel(sessionToken, ctx.channel());
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
        UserTunnel tunnel = UserTunnel.getManager().getUserTunnelByBindPort(inboundPort);
        if (tunnel == null) {
            return;
        }
        Channel serverChannel = tunnel.serverChannel();
        serverChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

}
