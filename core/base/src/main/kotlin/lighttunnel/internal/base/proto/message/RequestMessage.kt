package lighttunnel.internal.base.proto.message

import lighttunnel.TunnelRequest
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.proto.emptyBytes

class RequestMessage constructor(
    val request: TunnelRequest
) : ProtoMessage(ProtoMessageType.REQUEST, emptyBytes, request.toBytes()) {

    constructor(data: ByteArray) : this(TunnelRequest.fromBytes(data))

}