package lighttunnel.internal.base.proto.message

import lighttunnel.RemoteConnection
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.utils.LongUtils

class RemoteDisconnectMessage(
    val tunnelId: Long,
    val sessionId: Long,
    val conn: RemoteConnection
) : ProtoMessage(ProtoMessageType.REMOTE_DISCONNECT, LongUtils.toBytes(tunnelId, sessionId), conn.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(
        LongUtils.fromBytes(head, 0),
        LongUtils.fromBytes(head, 8),
        RemoteConnection.fromBytes(data)
    )

}
