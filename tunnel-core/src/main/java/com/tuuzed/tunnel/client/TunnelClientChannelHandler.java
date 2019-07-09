package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

/**
 * JProxy客户端数据通道处理器
 */
public class TunnelClientChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String mapping = ctx.channel().attr(ATTR_MAPPING).get();
        if (mapping != null) {
            TunnelClientChannelManager.getInstance().removeChannel(mapping);
        }
        super.channelInactive(ctx);

    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        logger.info("Recv message: {}", msg);
        switch (msg.getType()) {
            case MESSAGE_TYPE_HEARTBEAT:
                handleHeartbeatMessage(ctx, msg);
                break;
            case MESSAGE_TYPE_REQUEST_OPEN_TUNNEL:
                handleRequestOpenTunnelMessage(ctx, msg);
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
    private void handleRequestOpenTunnelMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
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
        TunnelClientChannelManager.getInstance().addChannel(mapping, ctx.channel());
    }

    /**
     * 处理数据透传消息
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
