package lighttunnel.base.proto.message

import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.proto.emptyBytes
import lighttunnel.base.utils.LongConv

class LocalConnectedMessage(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMessage(ProtoMessageType.LOCAL_CONNECTED, LongConv.toBytes(tunnelId, sessionId), emptyBytes) {

    constructor(head: ByteArray) : this(LongConv.fromBytes(head, 0), LongConv.fromBytes(head, 8))

}
