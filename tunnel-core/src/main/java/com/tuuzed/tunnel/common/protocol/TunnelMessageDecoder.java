package com.tuuzed.tunnel.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Tunnel消息解码器
 */
public class TunnelMessageDecoder extends LengthFieldBasedFrameDecoder {

    public TunnelMessageDecoder() {
        super(2 * 1024 * 1024, 0, TunnelMessageCodec.MESSAGE_FRAME_FIELD_SIZE, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        return TunnelMessageCodec.decode((ByteBuf) super.decode(ctx, in));
    }

}
