package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.asBytes
import lighttunnel.base.utils.asLong
import lighttunnel.base.utils.emptyBytes

class LocalDisconnectMsg(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMsg(ProtoMsgType.LOCAL_DISCONNECT, longArrayOf(tunnelId, sessionId).asBytes(), emptyBytes) {

    constructor(head: ByteArray) : this(head.asLong(0), head.asLong(8))

}
