package com.tuuzed.tunnel.server.stat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;

/**
 * 数据统计
 */
public class StatHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        StatItem statItem = StatItems.getInstance().getItem(sa.getPort());
        statItem.incrementReadBytes(((ByteBuf) msg).readableBytes());
        statItem.incrementReadMsgs(1);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        StatItem statItem = StatItems.getInstance().getItem(sa.getPort());
        statItem.incrementWriteBytes(((ByteBuf) msg).readableBytes());
        statItem.incrementWriteMsgs(1);
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        StatItem statItem = StatItems.getInstance().getItem(sa.getPort());
        statItem.incrementChannels();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        StatItem statItem = StatItems.getInstance().getItem(sa.getPort());
        statItem.decrementChannels();
        super.channelInactive(ctx);
    }

}
