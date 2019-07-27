package com.tuuzed.tunnel.http.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;

public class TunnelClientChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TunnelClientChannelHandler.class);


    private final LocalConnect localConnect;


    public TunnelClientChannelHandler(@NotNull LocalConnect localConnect) {
        this.localConnect = localConnect;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelActive: {}", ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelInactive: {}", ctx);
        super.channelInactive(ctx);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        // 服务器端数据流入
        logger.trace("channelRead0: {}", msg);
        switch (msg.getType()) {
            case TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE:
                handleOpenTunnelResponseMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
        }
    }


    private void handleOpenTunnelResponseMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final byte flag = head.readByte();
        final long tunnelToken = head.readLong();
        ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
        head.release();
        final OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(msg.getData());
        ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).set(openTunnelRequest);
    }


    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        logger.debug("tunnelToken: {}, sessionToken: {}", tunnelToken, sessionToken);
        ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);
        final OpenTunnelRequest openTunnelRequest = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).get();
        head.release();
        localConnect.writeDataToLocal(
                ctx.channel(), tunnelToken, sessionToken,
                openTunnelRequest.getLocalAddr(), openTunnelRequest.getLocalPort(),
                Unpooled.wrappedBuffer(msg.getData())
        );
    }
}
