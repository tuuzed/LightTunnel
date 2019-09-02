package tunnel2.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class ProtoMessageEncoder : MessageToByteEncoder<ProtoMessage>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: ProtoMessage?, outbuf: ByteBuf?) {
        msg ?: return
        outbuf ?: return
        ProtoMessage.encode(msg, outbuf)
    }
}