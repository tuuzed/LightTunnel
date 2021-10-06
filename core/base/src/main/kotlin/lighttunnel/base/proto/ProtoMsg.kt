@file:Suppress("FunctionName")

package lighttunnel.base.proto

import lighttunnel.base.RemoteConnection
import lighttunnel.base.TunnelRequest
import lighttunnel.base.utils.asBytes
import lighttunnel.base.utils.asLong

sealed class ProtoMsg(
    val type: ProtoMsgType,
    val head: ByteArray,
    val data: ByteArray
) {
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
        return "ProtoMsg(type=$type, head.length=${head.size}, data.length=${data.size})"
    }
}

object UnknownMsg : ProtoMsg(ProtoMsgType.UNKNOWN, emptyBytes, emptyBytes)
object ForceOffMsg : ProtoMsg(ProtoMsgType.FORCE_OFF, emptyBytes, emptyBytes)
object ForceOffReplyMsg : ProtoMsg(ProtoMsgType.FORCE_OFF_REPLY, emptyBytes, emptyBytes)
object HeartbeatPingMsg : ProtoMsg(ProtoMsgType.HEARTBEAT_PING, emptyBytes, emptyBytes)
object HeartbeatPongMsg : ProtoMsg(ProtoMsgType.HEARTBEAT_PONG, emptyBytes, emptyBytes)

class LocalConnectedMsg(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMsg(ProtoMsgType.LOCAL_CONNECTED, longArrayOf(tunnelId, sessionId).asBytes(), emptyBytes) {
    constructor(head: ByteArray) : this(head.asLong(0), head.asLong(8))
}

class LocalDisconnectMsg(
    val tunnelId: Long,
    val sessionId: Long
) : ProtoMsg(ProtoMsgType.LOCAL_DISCONNECT, longArrayOf(tunnelId, sessionId).asBytes(), emptyBytes) {
    constructor(head: ByteArray) : this(head.asLong(0), head.asLong(8))
}

class RemoteConnectedMsg(
    val tunnelId: Long,
    val sessionId: Long,
    val conn: RemoteConnection
) : ProtoMsg(ProtoMsgType.REMOTE_CONNECTED, longArrayOf(tunnelId, sessionId).asBytes(), conn.toBytes()) {
    constructor(head: ByteArray, data: ByteArray) : this(
        head.asLong(0),
        head.asLong(8),
        RemoteConnection.fromBytes(data)
    )
}

class RemoteDisconnectMsg(
    val tunnelId: Long,
    val sessionId: Long,
    val conn: RemoteConnection
) : ProtoMsg(ProtoMsgType.REMOTE_DISCONNECT, longArrayOf(tunnelId, sessionId).asBytes(), conn.toBytes()) {
    constructor(head: ByteArray, data: ByteArray) : this(
        head.asLong(0),
        head.asLong(8),
        RemoteConnection.fromBytes(data)
    )
}

class RequestMsg constructor(
    val request: TunnelRequest
) : ProtoMsg(ProtoMsgType.REQUEST, emptyBytes, request.toBytes()) {
    constructor(data: ByteArray) : this(TunnelRequest.fromBytes(data))
}

class ResponseErrMsg(
    val cause: Throwable
) : ProtoMsg(ProtoMsgType.RESPONSE_ERR, emptyBytes, cause.message.toString().toByteArray()) {
    constructor(data: ByteArray) : this(Throwable(String(data)))
}

class ResponseOkMsg(
    val tunnelId: Long,
    val request: TunnelRequest
) : ProtoMsg(ProtoMsgType.RESPONSE_OK, tunnelId.asBytes(), request.toBytes()) {
    constructor(head: ByteArray, data: ByteArray) : this(head.asLong(0), TunnelRequest.fromBytes(data))
}

class TransferMsg(
    val tunnelId: Long,
    val sessionId: Long,
    data: ByteArray
) : ProtoMsg(ProtoMsgType.TRANSFER, longArrayOf(tunnelId, sessionId).asBytes(), data) {
    constructor(
        head: ByteArray, data: ByteArray
    ) : this(head.asLong(0), head.asLong(8), data)
}




