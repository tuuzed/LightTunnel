package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes

class UnknownMessage : ProtoMessage(Type.FORCE_OFF, emptyBytes, emptyBytes)