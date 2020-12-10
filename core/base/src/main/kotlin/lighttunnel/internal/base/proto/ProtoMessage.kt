package lighttunnel.internal.base.proto

import lighttunnel.RemoteConnection
import lighttunnel.TunnelRequest
import lighttunnel.internal.base.proto.message.*

abstract class ProtoMessage internal constructor(
    val type: ProtoMessageType,
    val head: ByteArray,
    val data: ByteArray
) {

    @Suppress("FunctionName")
    companion object {
        private val UNKNOWN = UnknownMessage()
        private val PING = PingMessage()
        private val PONG = PongMessage()
        private val FORCE_OFF = ForceOffMessage()
        private val FORCE_OFF_REPLY = ForceOffReplyMessage()

        fun PING() = PING
        fun PONG() = PONG
        fun REQUEST(request: TunnelRequest) = RequestMessage(request)
        fun RESPONSE_OK(tunnelId: Long, request: TunnelRequest) = ResponseOkMessage(tunnelId, request)
        fun RESPONSE_ERR(cause: Throwable) = ResponseErrMessage(cause)
        fun TRANSFER(tunnelId: Long, sessionId: Long, data: ByteArray) = TransferMessage(tunnelId, sessionId, data)
        fun REMOTE_CONNECTED(tunnelId: Long, sessionId: Long, conn: RemoteConnection) =
            RemoteConnectedMessage(tunnelId, sessionId, conn)

        fun REMOTE_DISCONNECT(tunnelId: Long, sessionId: Long, conn: RemoteConnection) =
            RemoteDisconnectMessage(tunnelId, sessionId, conn)

        fun LOCAL_CONNECTED(tunnelId: Long, sessionId: Long) = LocalConnectedMessage(tunnelId, sessionId)
        fun LOCAL_DISCONNECT(tunnelId: Long, sessionId: Long) = LocalDisconnectMessage(tunnelId, sessionId)
        fun FORCE_OFF() = FORCE_OFF
        fun FORCE_OFF_REPLY() = FORCE_OFF_REPLY

        internal fun newInstance(type: ProtoMessageType, head: ByteArray, data: ByteArray): ProtoMessage {
            return when (type) {
                ProtoMessageType.UNKNOWN -> UNKNOWN
                ProtoMessageType.PING -> PING
                ProtoMessageType.PONG -> PONG
                ProtoMessageType.REQUEST -> RequestMessage(data)
                ProtoMessageType.RESPONSE_OK -> ResponseOkMessage(head, data)
                ProtoMessageType.RESPONSE_ERR -> ResponseErrMessage(data)
                ProtoMessageType.TRANSFER -> TransferMessage(head, data)
                ProtoMessageType.REMOTE_CONNECTED -> RemoteConnectedMessage(head, data)
                ProtoMessageType.REMOTE_DISCONNECT -> RemoteDisconnectMessage(head, data)
                ProtoMessageType.LOCAL_CONNECTED -> LocalConnectedMessage(head)
                ProtoMessageType.LOCAL_DISCONNECT -> LocalDisconnectMessage(head)
                ProtoMessageType.FORCE_OFF -> FORCE_OFF
                ProtoMessageType.FORCE_OFF_REPLY -> FORCE_OFF_REPLY
            }
        }

    }

    override fun toString(): String {
        return "ProtoMessage(type=$type, head.length=${head.size}, data.length=${data.size})"
    }


}
