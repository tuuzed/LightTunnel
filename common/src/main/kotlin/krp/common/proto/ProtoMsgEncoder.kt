package krp.common.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import krp.common.proto.msg.ProtoMsg

class ProtoMsgEncoder : MessageToByteEncoder<ProtoMsg>() {

    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext?, msg: ProtoMsg?, out: ByteBuf?) {
        msg ?: return
        out ?: return
        out.writeByte(Proto.HDR.toInt())
        out.writeByte(Proto.VERSION.toInt())
        out.writeByte(msg.flags.toInt())
        out.writeInt(msg.size)
        msg.transmit(out)
    }

}
