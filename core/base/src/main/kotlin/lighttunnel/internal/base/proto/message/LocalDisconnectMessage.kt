package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.proto.emptyBytes
import lighttunnel.internal.base.util.LongUtil

class LocalDisconnectMessage(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMessage(ProtoMessageType.LOCAL_DISCONNECT, LongUtil.toBytes(tunnelId, sessionId), emptyBytes) {

    constructor(head: ByteArray) : this(LongUtil.fromBytes(head, 0), LongUtil.fromBytes(head, 8))

}