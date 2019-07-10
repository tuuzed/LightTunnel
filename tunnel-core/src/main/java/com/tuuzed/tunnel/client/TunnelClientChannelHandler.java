package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
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
    @SuppressWarnings("Duplicates")
    private void handleOpenTunnelResponseMessage(ChannelHandlerContext ctx, TunnelMessage msg) {

    }

    /**
     * 处理数据透传消息
     * 数据流向: UserTunnel -> LocalTunnel
     */
    @SuppressWarnings("Duplicates")
    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        byte[] head = msg.getHead();
        String mapping = new String(head);
        logger.info("mapping: {}", mapping);
        String[] mappingTuple = mapping.split("<-");
        if (mappingTuple.length != 2) {
            ctx.close();
            return;
        }
        String localNetwork = mappingTuple[0];

        String[] localNetworkTuple = localNetwork.split(":");
        String localAddr = localNetworkTuple[0];
        int localPort = Integer.parseInt(localNetworkTuple[1]);
        LocalTunnel.getInstance().writeAndFlush(localAddr, localPort, msg.getData(), ctx.channel());
    }

}
