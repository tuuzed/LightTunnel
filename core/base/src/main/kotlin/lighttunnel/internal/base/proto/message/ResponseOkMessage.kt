package lighttunnel.internal.base.proto.message

import lighttunnel.TunnelRequest
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.utils.LongUtils

class ResponseOkMessage(
    val tunnelId: Long,
    val request: TunnelRequest
) : ProtoMessage(ProtoMessageType.RESPONSE_OK, LongUtils.toBytes(tunnelId), request.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(LongUtils.fromBytes(head, 0), TunnelRequest.fromBytes(data))

}
