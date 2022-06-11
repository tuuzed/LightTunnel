package krp.common.proto.msg

import io.netty.buffer.ByteBuf
import krp.common.proto.Proto.FLAG_ENCRYPTED
import krp.common.proto.Proto.FLAG_GZIP
import krp.common.utils.CryptoUtils
import krp.common.utils.unGZip
import kotlin.experimental.or

/**
 * 建立代理隧道请求
 *
 * 消息流向：Client -> Server
 */
class ProtoMsgRequest(
    private val data: ByteArray,
    aes128Key: ByteArray?,
    compressed: Boolean,
) : ProtoMsg {

    val payload: String by lazy {
        String(if (aes128Key != null) {
            CryptoUtils.decryptAES128(data, aes128Key)
        } else {
            data
        }.let { if (compressed) unGZip(it) else it })
    }
    override val flags: Byte = (if (aes128Key != null) FLAG_ENCRYPTED else 0) or (if (compressed) FLAG_GZIP else 0)
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Request
    override val size: Int get() = 1 + 4 + data.size

    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgRequest(payload='$payload')"
    }
}
