package lighttunnel.base.proto.message

import lighttunnel.base.RemoteConnection
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.utils.LongConv

class RemoteConnectedMessage(
    val tunnelId: Long,
    val sessionId: Long,
    val conn: RemoteConnection
) : ProtoMessage(ProtoMessageType.REMOTE_CONNECTED, LongConv.toBytes(tunnelId, sessionId), conn.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(
        LongConv.fromBytes(head, 0),
        LongConv.fromBytes(head, 8),
        RemoteConnection.fromBytes(data)
    )

}
