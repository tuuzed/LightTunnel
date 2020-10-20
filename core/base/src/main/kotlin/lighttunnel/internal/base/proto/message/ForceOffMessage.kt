package lighttunnel.internal.base.proto.message

import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes

class ForceOffMessage : ProtoMessage(Type.FORCE_OFF, emptyBytes, emptyBytes)