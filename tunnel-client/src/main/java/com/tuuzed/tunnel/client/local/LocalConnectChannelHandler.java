package com.tuuzed.tunnel.client.local;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.common.proto.ProtoMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地连接数据通道处理器
 */
@SuppressWarnings("Duplicates")
public class LocalConnectChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(LocalConnectChannelHandler.class);

    @NotNull
    private final LocalConnect localConnect;

    public LocalConnectChannelHandler(@NotNull LocalConnect localConnect) {
        this.localConnect = localConnect;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("channelInactive: {}", ctx);
        final Long tunnelToken = ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        if (tunnelToken != null && sessionToken != null) {
            Channel localChannel = localConnect.removeLocalChannel(tunnelToken, sessionToken);
            if (localChannel != null) {
                localChannel.close();
            }
            final Channel tunnelClientChannel = ctx.channel().attr(AttributeKeys.NEXT_CHANNEL).get();
            if (tunnelClientChannel != null) {
                tunnelClientChannel.writeAndFlush(
                    new ProtoMessage(ProtoMessage.Type.LOCAL_DISCONNECT,
                        Unpooled.copyLong(tunnelToken, sessionToken).array(),
                        null
                    )
                );
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.trace("exceptionCaught: {}", ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        final Long tunnelToken = ctx.channel().attr(AttributeKeys.TUNNEL_TOKEN).get();
        final Long sessionToken = ctx.channel().attr(AttributeKeys.SESSION_TOKEN).get();
        final Channel tunnelClientChannel = ctx.channel().attr(AttributeKeys.NEXT_CHANNEL).get();

        if (tunnelToken != null && sessionToken != null && tunnelClientChannel != null) {
            final byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            tunnelClientChannel.writeAndFlush(
                new ProtoMessage(ProtoMessage.Type.TRANSFER,
                    Unpooled.copyLong(tunnelToken, sessionToken).array(),
                    data
                )
            );
        }

    }
}