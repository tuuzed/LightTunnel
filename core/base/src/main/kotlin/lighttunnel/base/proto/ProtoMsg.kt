package lighttunnel.base.proto

import lighttunnel.base.RemoteConnection
import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.msg.*

abstract class ProtoMsg internal constructor(
    val type: ProtoMsgType,
    val head: ByteArray,
    val data: ByteArray
) {
    @Suppress("FunctionName")
    companion object {
        private fun UNKNOWN() = UnknownMsg
        fun HEARTBEAT_PING() = HeartbeatPingMsg
        fun HEARTBEAT_PONG() = HeartbeatPongMsg
        fun REQUEST(request: TunnelRequest) = RequestMsg(request)
        fun RESPONSE_OK(tunnelId: Long, request: TunnelRequest) = ResponseOkMsg(tunnelId, request)
        fun RESPONSE_ERR(cause: Throwable) = ResponseErrMsg(cause)
        fun TRANSFER(tunnelId: Long, sessionId: Long, data: ByteArray) = TransferMsg(tunnelId, sessionId, data)
        fun REMOTE_CONNECTED(
            tunnelId: Long, sessionId: Long, conn: RemoteConnection
        ) = RemoteConnectedMsg(tunnelId, sessionId, conn)

        fun REMOTE_DISCONNECT(
            tunnelId: Long, sessionId: Long, conn: RemoteConnection
        ) = RemoteDisconnectMsg(tunnelId, sessionId, conn)

        fun LOCAL_CONNECTED(tunnelId: Long, sessionId: Long) = LocalConnectedMsg(tunnelId, sessionId)
        fun LOCAL_DISCONNECT(tunnelId: Long, sessionId: Long) = LocalDisconnectMsg(tunnelId, sessionId)
        fun FORCE_OFF() = ForceOffMsg
        fun FORCE_OFF_REPLY() = ForceOffReplyMsg

        @Throws(Exception::class)
        internal fun newInstance(type: ProtoMsgType, head: ByteArray, data: ByteArray): ProtoMsg {
            return when (type) {
                ProtoMsgType.UNKNOWN -> UNKNOWN()
                ProtoMsgType.HEARTBEAT_PING -> HEARTBEAT_PING()
                ProtoMsgType.HEARTBEAT_PONG -> HEARTBEAT_PONG()
                ProtoMsgType.REQUEST -> RequestMsg(data)
                ProtoMsgType.RESPONSE_OK -> ResponseOkMsg(head, data)
                ProtoMsgType.RESPONSE_ERR -> ResponseErrMsg(data)
                ProtoMsgType.TRANSFER -> TransferMsg(head, data)
                ProtoMsgType.REMOTE_CONNECTED -> RemoteConnectedMsg(head, data)
                ProtoMsgType.REMOTE_DISCONNECT -> RemoteDisconnectMsg(head, data)
                ProtoMsgType.LOCAL_CONNECTED -> LocalConnectedMsg(head)
                ProtoMsgType.LOCAL_DISCONNECT -> LocalDisconnectMsg(head)
                ProtoMsgType.FORCE_OFF -> FORCE_OFF()
                ProtoMsgType.FORCE_OFF_REPLY -> FORCE_OFF_REPLY()
            }
        }

    }

    override fun toString(): String {
        return "ProtoMessage(type=$type, head.length=${head.size}, data.length=${data.size})"
    }

}
