package lighttunnel.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMassageDecoder : LengthFieldBasedFrameDecoder(
    4 * 1024 * 1024,
    0,
    ProtoConsts.TP_MESSAGE_LENGTH_FIELD_LENGTH,
    0,
    0,
    true
) {
    companion object {
        private const val MIN_BYTES =
            ProtoConsts.TP_MESSAGE_LENGTH_FIELD_LENGTH +
                ProtoConsts.TP_MESSAGE_COMMAND_LENGTH +
                ProtoConsts.TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        @Suppress("NAME_SHADOWING")
        val `in` = super.decode(ctx, `in`)
        if (`in` is ByteBuf) {
            if (`in`.readableBytes() < MIN_BYTES) return null
            val msgTotalLength = `in`.readInt()
            if (`in`.readableBytes() < msgTotalLength) return null
            // 开始解码数据
            val cmd = ProtoCommand.valueOf(`in`.readByte())
            val headLength = `in`.readInt()
            val head = ByteArray(headLength)
            `in`.readBytes(head)
            val dataLength = msgTotalLength -
                ProtoConsts.TP_MESSAGE_COMMAND_LENGTH -
                ProtoConsts.TP_MESSAGE_HEAD_LENGTH_FIELD_LENGTH -
                headLength
            val data = ByteArray(dataLength)
            `in`.readBytes(data)
            return ProtoMassage(cmd, head, data)
        } else {
            return `in`
        }
    }

}