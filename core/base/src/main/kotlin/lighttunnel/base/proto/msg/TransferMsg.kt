package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.asBytes
import lighttunnel.base.utils.asLong

class TransferMsg(
    val tunnelId: Long,
    val sessionId: Long,
    data: ByteArray
) : ProtoMsg(ProtoMsgType.TRANSFER, longArrayOf(tunnelId, sessionId).asBytes(), data) {

    constructor(
        head: ByteArray, data: ByteArray
    ) : this(head.asLong(0), head.asLong(8), data)

}
