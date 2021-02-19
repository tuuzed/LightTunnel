package lighttunnel.base.proto.message

import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.utils.LongConv

class ResponseOkMessage(
    val tunnelId: Long,
    val request: TunnelRequest
) : ProtoMessage(ProtoMessageType.RESPONSE_OK, LongConv.toBytes(tunnelId), request.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(LongConv.fromBytes(head, 0), TunnelRequest.fromBytes(data))

}
