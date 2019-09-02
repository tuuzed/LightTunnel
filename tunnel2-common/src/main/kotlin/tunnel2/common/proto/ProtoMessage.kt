package tunnel2.common.proto

import io.netty.buffer.ByteBuf


class ProtoMessage @JvmOverloads constructor(
    var cw: ProtoCw,
    var tunnelId: Long = 0,
    var sessionId: Long = 0,
    var data: ByteArray = EMPTY_BYTES
) {
    companion object {

        private val EMPTY_BYTES = ByteArray(0)

        /** 消息帧域长度 */
        const val MESSAGE_FRAME_FIELD_SIZE = 4
        /** 命令字长度 */
        private const val MESSAGE_CW_SIZE = 1
        /** 隧道ID长度 */
        private const val MESSAGE_TUNNEL_ID_SIZE = 8
        /** 会话ID长度 */
        private const val MESSAGE_SESSION_ID_SIZE = 8

        @JvmStatic
        @Throws(Exception::class)
        fun encode(message: ProtoMessage, outbuf: ByteBuf) {
            val pakTotalSize = MESSAGE_CW_SIZE + MESSAGE_TUNNEL_ID_SIZE + MESSAGE_SESSION_ID_SIZE + message.data.size
            // 数据包总长度
            outbuf.writeInt(pakTotalSize)
            outbuf.writeByte(message.cw.value.toInt())
            outbuf.writeLong(message.tunnelId)
            outbuf.writeLong(message.sessionId)
            outbuf.writeBytes(message.data)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun decode(inbuf: ByteBuf?): ProtoMessage? {
            inbuf ?: return null
            // 判断可读数据是否满足读取消息帧长度添加
            if (inbuf.readableBytes() < MESSAGE_FRAME_FIELD_SIZE) return null
            val pakTotalSize = inbuf.readInt()
            if (inbuf.readableBytes() < pakTotalSize) return null
            // 开始解码数据
            val cw = ProtoCw.ofValue(inbuf.readByte())
            val tunnelId = inbuf.readLong()
            val sessionId = inbuf.readLong()
            val dataSize = pakTotalSize - MESSAGE_CW_SIZE - MESSAGE_TUNNEL_ID_SIZE - MESSAGE_SESSION_ID_SIZE
            val data = ByteArray(dataSize)
            inbuf.readBytes(data)
            return ProtoMessage(cw = cw, tunnelId = tunnelId, sessionId = sessionId, data = data)
        }
    }
}