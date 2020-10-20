package lighttunnel.internal.base.proto

import lighttunnel.RemoteConnection
import lighttunnel.TunnelRequest
import lighttunnel.internal.base.proto.message.*

abstract class ProtoMessage internal constructor(
    val type: Type,
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
        fun REMOTE_CONNECTED(tunnelId: Long, sessionId: Long, conn: RemoteConnection) = RemoteConnectedMessage(tunnelId, sessionId, conn)
        fun REMOTE_DISCONNECT(tunnelId: Long, sessionId: Long, conn: RemoteConnection) = RemoteDisconnectMessage(tunnelId, sessionId, conn)
        fun LOCAL_CONNECTED(tunnelId: Long, sessionId: Long) = LocalConnectedMessage(tunnelId, sessionId)
        fun LOCAL_DISCONNECT(tunnelId: Long, sessionId: Long) = LocalDisconnectMessage(tunnelId, sessionId)
        fun FORCE_OFF() = FORCE_OFF
        fun FORCE_OFF_REPLY() = FORCE_OFF_REPLY

        internal fun newInstance(type: Type, head: ByteArray, data: ByteArray): ProtoMessage {
            return when (type) {
                Type.UNKNOWN -> UNKNOWN
                Type.PING -> PING
                Type.PONG -> PONG
                Type.REQUEST -> RequestMessage(data)
                Type.RESPONSE_OK -> ResponseOkMessage(head, data)
                Type.RESPONSE_ERR -> ResponseErrMessage(data)
                Type.TRANSFER -> TransferMessage(head, data)
                Type.REMOTE_CONNECTED -> RemoteConnectedMessage(head, data)
                Type.REMOTE_DISCONNECT -> RemoteDisconnectMessage(head, data)
                Type.LOCAL_CONNECTED -> LocalConnectedMessage(head)
                Type.LOCAL_DISCONNECT -> LocalDisconnectMessage(head)
                Type.FORCE_OFF -> FORCE_OFF
                Type.FORCE_OFF_REPLY -> FORCE_OFF_REPLY
            }
        }

    }

    override fun toString(): String {
        return "ProtoMessage(type=$type, head.length=${head.size}, data.length=${data.size})"
    }

    enum class Type(val code: Byte) {
        /**
         * 未知
         */
        UNKNOWN(0x00.toByte()),

        /**
         * 心跳消息 PING
         * 消息流向：Client <-> Server
         */
        PING(0x01.toByte()),

        /**
         * 心跳消息 PONG
         * 消息流向：Client <-> Server
         */
        PONG(0x02.toByte()),

        /**
         * 建立隧道请求
         * 消息流向：Client -> Server
         */
        REQUEST(0x10.toByte()),

        /**
         * 建立隧道响应成功
         * 消息流向：Client <- Server
         */
        RESPONSE_OK(0x20.toByte()),

        /**
         * 建立隧道响应失败
         * 消息流向：Client <- Server
         */
        RESPONSE_ERR(0x21.toByte()),

        /**
         * 透传消息
         * 消息流向：Client <-> Server
         */
        TRANSFER(0x30.toByte()),

        /**
         * 远程连接成功
         * 消息流向：Client <- Server
         */
        REMOTE_CONNECTED(0x40.toByte()),

        /**
         * 远程断开连接
         * 消息流向：Client <- Server
         */
        REMOTE_DISCONNECT(0x41.toByte()),

        /**
         * 本地连接成功
         * 消息流向：Client -> Server
         */
        LOCAL_CONNECTED(0x42.toByte()),

        /**
         * 本地连接断开
         * 消息流向：Client -> Server
         */
        LOCAL_DISCONNECT(0x43.toByte()),

        /**
         * 强制下线
         * 消息流向：Server -> Client
         */
        FORCE_OFF(0x50.toByte()),

        /**
         * 强制下线回复
         * 消息流向：Client -> Server
         */
        FORCE_OFF_REPLY(0x51.toByte())
        ;

        companion object {
            fun ofCode(code: Byte) = values().firstOrNull { it.code == code } ?: UNKNOWN
        }

    }

}
