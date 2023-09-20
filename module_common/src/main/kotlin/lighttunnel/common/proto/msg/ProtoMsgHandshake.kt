package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf
import lighttunnel.common.proto.Proto.FLAG_GZIP
import lighttunnel.common.proto.Proto.FLAG_NONE
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
        if (compressed) CompressUtils.decompress(data) else data
    }

    override val flag: Byte = (if (compressed) FLAG_GZIP else FLAG_NONE)
    override val cmd: ProtoMsg.Cmd get() = ProtoMsg.Cmd.Handshake
    override val size: Int get() = 1 + 4 + data.size

    override fun transmit(out: ByteBuf) {
        out.writeByte(cmd.value.toInt())
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgHandshake(data='${data.size}Bytes')"
    }
}
