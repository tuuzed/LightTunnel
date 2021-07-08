package lighttunnel.base.proto.msg

import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

class RequestMsg constructor(
    val request: TunnelRequest
) : ProtoMsg(ProtoMsgType.REQUEST, emptyBytes, request.toBytes()) {

    constructor(data: ByteArray) : this(TunnelRequest.fromBytes(data))

}
