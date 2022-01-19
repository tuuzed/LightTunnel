package lighttunnel.base.proto

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class ProtoMsgDecoder : LengthFieldBasedFrameDecoder(
    1024 * 1024,
    0,
    4
) {

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?): Any? {
        val frame = super.decode(ctx, `in`)
        if (frame is ByteBuf) {
            val totalLength = frame.readInt()
            val type = ProtoMsgType.codeOf(frame.readByte())
            val headLength = frame.readInt()
            val head = ByteArray(headLength)
            frame.readBytes(head)
            val dataLength = totalLength -
                PROTO_MESSAGE_TYPE_LENGTH -
                PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH -
                headLength
            val data = ByteArray(dataLength)
            frame.readBytes(data)
            return ProtoMsg.newInstance(type, head, data)
        }
        return frame
    }

}
