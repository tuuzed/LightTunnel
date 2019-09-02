package tunnel2.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class ProtoMessageEncoder : MessageToByteEncoder<ProtoMessage>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: ProtoMessage?, outbuf: ByteBuf?) {
        if (msg != null && outbuf != null) {
            ProtoMessage.encode(msg, outbuf)
        }
    }
}