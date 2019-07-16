package com.tuuzed.tunnel.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Tunnel消息编码器
 */
public class TunnelMessageEncoder extends MessageToByteEncoder<TunnelMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelMessage msg, ByteBuf out) throws Exception {
        TunnelMessageCodec.encode(msg, out);
    }

}
