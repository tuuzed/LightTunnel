package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * JProxy服务数据通道处理器
 */
public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        UserTunnel.getManager().closeUserTunnel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.info("Recv: {}", msg);
        switch (msg.getType()) {
            case MESSAGE_TYPE_HEARTBEAT:
                handleHeartbeatMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                handleOpenTunnelRequestMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        ctx.channel().writeAndFlush(TunnelMessage.newInstance(MESSAGE_TYPE_HEARTBEAT));
    }

    /**
     * 处理建立隧道请求消息
     */
    private void handleOpenTunnelRequestMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        byte[] head = msg.getHead();
        String mapping = new String(head);
        logger.info("mapping: {}", mapping);
        String[] mappingTuple = mapping.split("<-");
        if (mappingTuple.length != 2) {
            ctx.close();
            return;
        }
        String localNetwork = mappingTuple[0];
        int remotePort = Integer.parseInt(mappingTuple[1]);

        ctx.channel().attr(ATTR_MAPPING).set(mapping);
        ctx.channel().attr(ATTR_LOCAL_NETWORK).set(localNetwork);
        ctx.channel().attr(ATTR_REMOTE_PORT).set(remotePort);
        long tunnelToken = UserTunnel.getManager().openUserTunnel(remotePort, ctx.channel());
        ctx.writeAndFlush(
                TunnelMessage.newInstance(MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                        .setHead(Unpooled.copyLong(tunnelToken).array())
        );
    }

    /**
     * 处理数据透传消息
     * 数据流向: TunnelClient  ->  UserTunnel
     */
    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        long tunnelToken = head.readLong();
        long sessionToken = head.readLong();
        UserTunnel tunnel = UserTunnel.getManager().getUserTunnelByTunnelToken(tunnelToken);
        if (tunnel != null) {
            Channel userTunnelChannel = tunnel.getUserTunnelChannel(sessionToken);
            if (userTunnelChannel != null) {
                userTunnelChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
            }
        }
    }

}
