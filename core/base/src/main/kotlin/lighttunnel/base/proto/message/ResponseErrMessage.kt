package lighttunnel.base.proto.message

import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.proto.emptyBytes

class ResponseErrMessage(
    val cause: Throwable
) : ProtoMessage(ProtoMessageType.RESPONSE_ERR, emptyBytes, cause.message.toString().toByteArray()) {

    constructor(data: ByteArray) : this(Throwable(String(data)))

}
