package lighttunnel.base.proto.msg

import lighttunnel.base.RemoteConnection
import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.asBytes
import lighttunnel.base.utils.asLong

class RemoteConnectedMsg(
    val tunnelId: Long,
    val sessionId: Long,
    val conn: RemoteConnection
) : ProtoMsg(ProtoMsgType.REMOTE_CONNECTED, longArrayOf(tunnelId, sessionId).asBytes(), conn.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(
        head.asLong(0),
        head.asLong(8),
        RemoteConnection.fromBytes(data)
    )

}
