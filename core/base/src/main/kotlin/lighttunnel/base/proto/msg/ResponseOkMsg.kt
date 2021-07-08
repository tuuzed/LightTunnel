package lighttunnel.base.proto.msg

import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.asBytes
import lighttunnel.base.utils.asLong

class ResponseOkMsg(
    val tunnelId: Long,
    val request: TunnelRequest
) : ProtoMsg(ProtoMsgType.RESPONSE_OK, tunnelId.asBytes(), request.toBytes()) {

    constructor(head: ByteArray, data: ByteArray) : this(head.asLong(0), TunnelRequest.fromBytes(data))

}
