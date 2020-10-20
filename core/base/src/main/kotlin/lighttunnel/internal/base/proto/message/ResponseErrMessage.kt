package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes

class ResponseErrMessage(
    val cause: Throwable
) : ProtoMessage(Type.RESPONSE_ERR, emptyBytes, cause.message.toString().toByteArray()) {

    constructor(data: ByteArray) : this(Throwable(String(data)))

}