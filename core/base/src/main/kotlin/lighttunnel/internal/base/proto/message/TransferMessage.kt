package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.utils.LongUtils

class TransferMessage(
    val tunnelId: Long,
    val sessionId: Long,
    data: ByteArray
) : ProtoMessage(ProtoMessageType.TRANSFER, LongUtils.toBytes(tunnelId, sessionId), data) {

    constructor(head: ByteArray, data: ByteArray) : this(
        LongUtils.fromBytes(head, 0),
        LongUtils.fromBytes(head, 8),
        data
    )

}
