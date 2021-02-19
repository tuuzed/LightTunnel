package lighttunnel.base.proto.message

import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.utils.LongConv

class TransferMessage(
    val tunnelId: Long,
    val sessionId: Long,
    data: ByteArray
) : ProtoMessage(ProtoMessageType.TRANSFER, LongConv.toBytes(tunnelId, sessionId), data) {

    constructor(head: ByteArray, data: ByteArray) : this(
        LongConv.fromBytes(head, 0),
        LongConv.fromBytes(head, 8),
        data
    )

}
