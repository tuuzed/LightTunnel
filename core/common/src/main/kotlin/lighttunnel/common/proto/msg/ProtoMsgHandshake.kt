package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf
import lighttunnel.common.proto.Proto
import lighttunnel.common.utils.CompressUtils

/**
 * 握手，交互加密秘钥
 *
 * 消息流向：Client <-> Server
 */
class ProtoMsgHandshake(
    private val data: ByteArray,
    compressed: Boolean,
) : ProtoMsg {

    val rawBytes: ByteArray by lazy {
        if (compressed) CompressUtils.unGZip(data) else data
    }

    override val flags: Byte = (if (compressed) Proto.FLAG_GZIP else 0)
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Handshake
    override val size: Int get() = 1 + 4 + data.size

    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgHandshake(data='${data.size}Bytes')"
    }
}
