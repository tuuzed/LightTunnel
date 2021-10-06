package lighttunnel.base.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMsgDecoder : LengthFieldBasedFrameDecoder(
    4 * 1024 * 1024,
    0,
    PROTO_MESSAGE_LENGTH_FIELD_LENGTH,
    0,
    0,
    true
) {

    companion object {
        private const val MIN_BYTES = PROTO_MESSAGE_LENGTH_FIELD_LENGTH +
            PROTO_MESSAGE_TYPE_LENGTH +
            PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH
    }

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, input: ByteBuf?): Any? {
        when (val rst = super.decode(ctx, input)) {
            is ByteBuf -> {
                if (rst.readableBytes() < MIN_BYTES) return null
                val totalLength = rst.readInt()
                if (rst.readableBytes() < totalLength) return null
                // 开始解码数据
                val type = ProtoMsgType.codeOf(rst.readByte())
                val headLength = rst.readInt()
                val head = ByteArray(headLength)
                rst.readBytes(head)
                val dataLength = totalLength -
                    PROTO_MESSAGE_TYPE_LENGTH -
                    PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH -
                    headLength
                val data = ByteArray(dataLength)
                rst.readBytes(data)
                return ProtoMsg.newInstance(type, head, data)
            }
            else -> return rst
        }
    }

}
