package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.util.LongUtil

class TransferMessage(
    val tunnelId: Long,
    val sessionId: Long,
    data: ByteArray
) : ProtoMessage(Type.TRANSFER, LongUtil.toBytes(tunnelId, sessionId), data) {

    constructor(head: ByteArray, data: ByteArray) : this(LongUtil.fromBytes(head, 0), LongUtil.fromBytes(head, 8), data)

}