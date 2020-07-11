package lighttunnel.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMessageDecoder : LengthFieldBasedFrameDecoder(
    4 * 1024 * 1024,
    0,
    ProtoConsts.PROTO_MESSAGE_LENGTH_FIELD_LENGTH,
    0,
    0,
    true
) {
    companion object {
        private const val MIN_BYTES =
            ProtoConsts.PROTO_MESSAGE_LENGTH_FIELD_LENGTH +
                ProtoConsts.PROTO_MESSAGE_TYPE_LENGTH +
                ProtoConsts.PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        @Suppress("NAME_SHADOWING")
        val `in` = super.decode(ctx, `in`)
        if (`in` is ByteBuf) {
            if (`in`.readableBytes() < MIN_BYTES) return null
            val totalLength = `in`.readInt()
            if (`in`.readableBytes() < totalLength) return null
            // 开始解码数据
            val type = ProtoMessageType.codeOf(`in`.readByte())
            val headLength = `in`.readInt()
            val head = ByteArray(headLength)
            `in`.readBytes(head)
            val dataLength = totalLength -
                ProtoConsts.PROTO_MESSAGE_TYPE_LENGTH -
                ProtoConsts.PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH -
                headLength
            val data = ByteArray(dataLength)
            `in`.readBytes(data)
            return ProtoMessage(type, head, data)
        } else {
            return `in`
        }
    }

}