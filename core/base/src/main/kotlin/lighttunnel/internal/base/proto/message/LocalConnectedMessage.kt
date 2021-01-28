package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.proto.emptyBytes
import lighttunnel.internal.base.utils.LongUtils

class LocalConnectedMessage(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMessage(ProtoMessageType.LOCAL_CONNECTED, LongUtils.toBytes(tunnelId, sessionId), emptyBytes) {

    constructor(head: ByteArray) : this(LongUtils.fromBytes(head, 0), LongUtils.fromBytes(head, 8))

}
