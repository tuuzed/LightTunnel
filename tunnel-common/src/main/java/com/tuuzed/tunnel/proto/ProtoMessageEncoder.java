package com.tuuzed.tunnel.proto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtoMessageEncoder extends MessageToByteEncoder<ProtoMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoMessage msg, ByteBuf out) throws Exception {
        ProtoMessageCodec.encode(msg, out);
    }

}
