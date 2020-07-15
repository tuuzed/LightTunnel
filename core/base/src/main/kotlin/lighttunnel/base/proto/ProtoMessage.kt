package lighttunnel.base.proto

import lighttunnel.base.util.LongUtil
import lighttunnel.base.util.emptyBytes

class ProtoMessage(
    val type: ProtoMessageType,
    val head: ByteArray = emptyBytes,
    val data: ByteArray = emptyBytes
) {

    val tunnelId by lazy { LongUtil.fromBytes(head, 0) }
    val sessionId by lazy { LongUtil.fromBytes(head, 8) }

    override fun toString(): String {
        return "ProtoMessage(type=$type, head.length=${head.size}, data.length=${data.size})"
    }


}