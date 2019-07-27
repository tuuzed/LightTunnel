package com.tuuzed.tunnel.http.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class TunnelServerChannelHandler extends SimpleChannelInboundHandler<TunnelMessage> {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServerChannelHandler.class);
    private final AtomicLong tunnelTokenProducer = new AtomicLong(0);


    private final TunnelServerChannels tunnelServerChannels;
    private final HttpServerChannels httpServerChannels;

    public TunnelServerChannelHandler(
            @NotNull TunnelServerChannels httpTunnelChannels,
            @NotNull HttpServerChannels httpServerChannels
    ) {
        this.tunnelServerChannels = httpTunnelChannels;
        this.httpServerChannels = httpServerChannels;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelInactive: {}", ctx);
        String subDomain = ctx.channel().attr(TunnelAttributeKey.SUB_DOMAIN).get();
        if (subDomain != null) {
            Channel channel = tunnelServerChannels.removeChannel(subDomain);
            if (channel != null) {
                channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        switch (msg.getType()) {
            case TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_REQUEST:
                handleOpenTunnelRequestMessage(ctx, msg);
                break;
            case TunnelMessage.MESSAGE_TYPE_TRANSFER:
                handleTransferMessage(ctx, msg);
                break;
        }
    }

    private void handleOpenTunnelRequestMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        OpenTunnelRequest openTunnelRequest = OpenTunnelRequest.fromBytes(msg.getHead());
        logger.debug("openTunnelRequest: {}", openTunnelRequest);
        tunnelServerChannels.putChannel(openTunnelRequest.getSubDomain(), ctx.channel());
        final long tunnelToken = tunnelTokenProducer.incrementAndGet();
        ctx.channel().attr(TunnelAttributeKey.SUB_DOMAIN).set(openTunnelRequest.getSubDomain());
        ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
        ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN_PRODUCER).set(new AtomicLong());
        final ByteBuf head = Unpooled.buffer(9);
        head.writeByte(TunnelMessage.OPEN_TUNNEL_RESPONSE_SUCCESS);
        head.writeLong(tunnelToken);
        ctx.writeAndFlush(
                TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_RESPONSE)
                        .setHead(head.array())
                        .setData(openTunnelRequest.toBytes())
        );
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {
        final ByteBuf head = Unpooled.wrappedBuffer(msg.getHead());
        final long tunnelToken = head.readLong();
        final long sessionToken = head.readLong();
        head.release();
        final Channel nextChannel = httpServerChannels.getChannel(tunnelToken, sessionToken);
        if (nextChannel != null) {
            nextChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.getData()));
        }
    }

}
