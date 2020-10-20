package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes

class PongMessage : ProtoMessage(Type.PONG, emptyBytes, emptyBytes)