package com.tuuzed.tunnel.client;

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
 * Tunnel客户端数据通道处理器
 */
public class TunnelClientChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 隧道断开
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.info("Recv : {}", msg);
        switch (msg.getType()) {
            case MESSAGE_TYPE_HEARTBEAT:
                handleHeartbeatMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                handleOpenTunnelResponseMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
            default:
                break;
        }
    }

    /**
     * 处理心跳数据
     */
    private void handleHeartbeatMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        // TODO
    }

    /**
     * 处理建立隧道请求消息
     */
    private void handleOpenTunnelResponseMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        byte[] head = msg.getHead();
        long tunnelToken = Unpooled.wrappedBuffer(head).readLong();
        ctx.channel().attr(ATTR_TUNNEL_TOKNE).set(tunnelToken);
    }

    /**
     * 处理数据透传消息
     * 数据流向: UserTunnel -> LocalTunnel
     */
    private void handleTransferMessage(final ChannelHandlerContext ctx, final TunnelMessage msg) {
        ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        long tunnelToken = head.readLong();
        long sessionToken = head.readLong();
        ctx.channel().attr(ATTR_TUNNEL_TOKNE).set(tunnelToken);
        ctx.channel().attr(ATTR_SESSION_TOKNE).set(sessionToken);
        String localNetwork = ctx.channel().attr(ATTR_LOCAL_NETWORK).get();
        String[] localNetworkTuple = localNetwork.split(":");
        String localAddr = localNetworkTuple[0];
        int localPort = Integer.parseInt(localNetworkTuple[1]);
        LocalTunnel.getInstance().getLocalTunnelChannel(localAddr, localPort, tunnelToken, sessionToken,
                new LocalTunnel.Callback<Channel>() {
                    @Override
                    public void invoke(Channel channel) {
                        channel.attr(ATTR_NEXT_CHANNEL).set(ctx.channel());
                        channel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
                    }
                }
        );
    }

}
