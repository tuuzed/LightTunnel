package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

object HeartbeatPongMsg : ProtoMsg(ProtoMsgType.HEARTBEAT_PONG, emptyBytes, emptyBytes)
