package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes
import lighttunnel.internal.base.util.LongUtil

class LocalConnectedMessage(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMessage(Type.LOCAL_CONNECTED, LongUtil.toBytes(tunnelId, sessionId), emptyBytes) {

    constructor(head: ByteArray) : this(LongUtil.fromBytes(head, 0), LongUtil.fromBytes(head, 8))

}