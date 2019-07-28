package com.tuuzed.tunnel.server.stats;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * 数据统计
 */
public class StatsHandler extends ChannelDuplexHandler {

    @NotNull
    private final Stats stats;

    public StatsHandler(@NotNull Stats stats) {
        this.stats = stats;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        Stats.Item item = stats.getItem(sa.getPort());
        item.incrementReadBytes(((ByteBuf) msg).readableBytes());
        item.incrementReadMsgs(1);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        Stats.Item item = stats.getItem(sa.getPort());
        item.incrementWriteBytes(((ByteBuf) msg).readableBytes());
        item.incrementWriteMsgs(1);
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        Stats.Item item = stats.getItem(sa.getPort());
        item.incrementChannels();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        Stats.Item item = stats.getItem(sa.getPort());
        item.decrementChannels();
        super.channelInactive(ctx);
    }

}
