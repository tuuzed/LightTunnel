package tunnel2.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMessageDecoder : LengthFieldBasedFrameDecoder(
    4 * 1024 * 1024,
    0,
    /* lengthFieldLength */  ProtoMessage.MESSAGE_FRAME_FIELD_SIZE,
    0,
    0,
    true
) {
    override fun decode(ctx: ChannelHandlerContext?, inbuf: ByteBuf?): Any? {
        return ProtoMessage.decode(inbuf)
    }
}