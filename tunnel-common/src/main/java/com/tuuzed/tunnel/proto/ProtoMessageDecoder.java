package com.tuuzed.tunnel.proto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class ProtoMessageDecoder extends LengthFieldBasedFrameDecoder {

    public ProtoMessageDecoder() {
        super(
            2 * 1024 * 1024,
            0,
            ProtoMessageCodec.MESSAGE_FRAME_FIELD_SIZE,
            0,
            0
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        final ByteBuf newIn = (ByteBuf) super.decode(ctx, in);
        final ProtoMessage message = ProtoMessageCodec.decode(newIn);
        if (newIn != null) {
            newIn.release();
        }
        return message;
    }

}
