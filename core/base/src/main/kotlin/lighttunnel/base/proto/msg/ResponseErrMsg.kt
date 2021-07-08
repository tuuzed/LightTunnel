package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

class ResponseErrMsg(
    val cause: Throwable
) : ProtoMsg(ProtoMsgType.RESPONSE_ERR, emptyBytes, cause.message.toString().toByteArray()) {

    constructor(data: ByteArray) : this(Throwable(String(data)))

}
