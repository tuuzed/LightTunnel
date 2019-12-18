package lighttunnel.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class LTMassageEncoder : MessageToByteEncoder<LTMassage>() {

    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext?, msg: LTMassage?, outbuf: ByteBuf?) {
        msg ?: return
        outbuf ?: return
        val msgTotalLength = TP_MESSAGE_COMMAND_LENGTH +
                TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH +
                msg.head.size +
                msg.data.size
        outbuf.writeInt(msgTotalLength)
        outbuf.writeByte(msg.cmd.value.toInt())
        outbuf.writeInt(msg.head.size)
        outbuf.writeBytes(msg.head)
        outbuf.writeBytes(msg.data)
    }
}