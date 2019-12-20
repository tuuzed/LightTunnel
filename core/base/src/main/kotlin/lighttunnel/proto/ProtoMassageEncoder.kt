package lighttunnel.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class ProtoMassageEncoder : MessageToByteEncoder<ProtoMassage>() {

    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext?, msg: ProtoMassage?, out: ByteBuf?) {
        msg ?: return
        out ?: return
        val msgTotalLength = ProtoConsts.TP_MESSAGE_COMMAND_LENGTH +
            ProtoConsts.TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH +
            msg.head.size +
            msg.data.size
        out.writeInt(msgTotalLength)
        out.writeByte(msg.cmd.flag.toInt())
        out.writeInt(msg.head.size)
        out.writeBytes(msg.head)
        out.writeBytes(msg.data)
    }
}