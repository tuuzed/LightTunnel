package lighttunnel.base.proto.message

import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.proto.emptyBytes

class PingMessage : ProtoMessage(ProtoMessageType.PING, emptyBytes, emptyBytes)
