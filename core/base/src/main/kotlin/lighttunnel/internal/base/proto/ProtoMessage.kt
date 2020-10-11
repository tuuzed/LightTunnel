package lighttunnel.internal.base.proto

import lighttunnel.RemoteConnection
import lighttunnel.TunnelRequest
import lighttunnel.internal.base.util.LongUtil

class ProtoMessage private constructor(
    val type: Type,
    val head: ByteArray,
    val data: ByteArray
) {

    @Suppress("FunctionName")
    companion object {
        private val PING = ProtoMessage(Type.PING, emptyBytes, emptyBytes)
        private val PONG = ProtoMessage(Type.PONG, emptyBytes, emptyBytes)
        private val FORCE_OFF = ProtoMessage(Type.FORCE_OFF, emptyBytes, emptyBytes)
        private val FORCE_OFF_REPLY = ProtoMessage(Type.FORCE_OFF_REPLY, emptyBytes, emptyBytes)

        fun PING() = PING
        fun PONG() = PONG
        fun REQUEST(request: TunnelRequest) = ProtoMessage(Type.REQUEST, emptyBytes, request.toBytes())
        fun RESPONSE_OK(tunnelId: Long, request: TunnelRequest) = ProtoMessage(Type.RESPONSE_OK, LongUtil.toBytes(tunnelId), request.toBytes())
        fun RESPONSE_ERR(cause: Throwable) = ProtoMessage(Type.RESPONSE_ERR, emptyBytes, cause.message.toString().toByteArray())
        fun TRANSFER(tunnelId: Long, sessionId: Long, data: ByteArray) = ProtoMessage(Type.TRANSFER, LongUtil.toBytes(tunnelId, sessionId), data)
        fun REMOTE_CONNECTED(tunnelId: Long, sessionId: Long, conn: RemoteConnection) = ProtoMessage(Type.REMOTE_CONNECTED, LongUtil.toBytes(tunnelId, sessionId), conn.toBytes())
        fun REMOTE_DISCONNECT(tunnelId: Long, sessionId: Long, conn: RemoteConnection) = ProtoMessage(Type.REMOTE_DISCONNECT, LongUtil.toBytes(tunnelId, sessionId), conn.toBytes())
        fun LOCAL_CONNECTED(tunnelId: Long, sessionId: Long) = ProtoMessage(Type.LOCAL_CONNECTED, LongUtil.toBytes(tunnelId, sessionId), emptyBytes)
        fun LOCAL_DISCONNECT(tunnelId: Long, sessionId: Long) = ProtoMessage(Type.LOCAL_DISCONNECT, LongUtil.toBytes(tunnelId, sessionId), emptyBytes)
        fun FORCE_OFF() = FORCE_OFF
        fun FORCE_OFF_REPLY() = FORCE_OFF_REPLY

        internal fun newInstance(type: Type, head: ByteArray, data: ByteArray) = ProtoMessage(type, head, data)
    }

    val tunnelId by lazy { LongUtil.fromBytes(head, 0) }
    val sessionId by lazy { LongUtil.fromBytes(head, 8) }

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
