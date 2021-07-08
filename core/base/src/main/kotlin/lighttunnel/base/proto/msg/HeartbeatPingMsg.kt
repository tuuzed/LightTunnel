package lighttunnel.base.proto.msg

import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.ProtoMsgType
import lighttunnel.base.utils.emptyBytes

object HeartbeatPingMsg : ProtoMsg(ProtoMsgType.HEARTBEAT_PING, emptyBytes, emptyBytes)
