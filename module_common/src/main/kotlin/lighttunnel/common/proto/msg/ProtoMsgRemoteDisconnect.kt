package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf
import lighttunnel.common.proto.Proto.FLAG_ENCRYPTED
import lighttunnel.common.proto.Proto.FLAG_GZIP
import lighttunnel.common.proto.Proto.FLAG_NONE
import lighttunnel.common.utils.CryptoUtils
import lighttunnel.common.utils.CompressUtils
import kotlin.experimental.or

/**
 * 远程断开连接
 *
 * 消息流向：Client <- Server
 */
class ProtoMsgRemoteDisconnect(
    val tunnelId: Long,
    val sessionId: Long,
    private val data: ByteArray,
    aes128Key: ByteArray?,
    compressed: Boolean,
) : ProtoMsg {

    val payload: String by lazy {
        String(if (aes128Key != null) {
            CryptoUtils.decryptAES128(data, aes128Key)
        } else {
            data
        }.let { if (compressed) CompressUtils.decompress(it) else it })
    }
    override val flag: Byte =
        (if (aes128Key != null) FLAG_ENCRYPTED else FLAG_NONE) or (if (compressed) FLAG_GZIP else FLAG_NONE)
    override val cmd: ProtoMsg.Cmd get() = ProtoMsg.Cmd.RemoteDisconnect
    override val size: Int get() = 1 + 8 + 8 + 4 + data.size

    override fun transmit(out: ByteBuf) {
        out.writeByte(cmd.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgRemoteDisconnect(tunnelId=$tunnelId, sessionId=$sessionId, payload='$payload')"
    }
}
