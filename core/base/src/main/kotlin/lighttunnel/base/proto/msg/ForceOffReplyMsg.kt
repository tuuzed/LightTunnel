package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

object ForceOffReplyMsg : ProtoMsg(ProtoMsgType.FORCE_OFF_REPLY, emptyBytes, emptyBytes)
