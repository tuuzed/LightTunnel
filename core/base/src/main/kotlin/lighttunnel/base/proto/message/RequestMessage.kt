package lighttunnel.base.proto.message

import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.proto.emptyBytes

class RequestMessage constructor(
    val request: TunnelRequest
) : ProtoMessage(ProtoMessageType.REQUEST, emptyBytes, request.toBytes()) {

    constructor(data: ByteArray) : this(TunnelRequest.fromBytes(data))

}
