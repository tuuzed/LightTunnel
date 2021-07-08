package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

object ForceOffMsg : ProtoMsg(ProtoMsgType.FORCE_OFF, emptyBytes, emptyBytes)
