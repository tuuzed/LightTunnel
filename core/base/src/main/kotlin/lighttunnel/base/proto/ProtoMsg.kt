@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package lighttunnel.base.proto

import io.netty.buffer.ByteBuf
import lighttunnel.base.utils.contentToHexString

sealed interface ProtoMsg {
    val type: Type
    val size: Int
    fun transmit(out: ByteBuf)

    companion object {
        private val mappings = Type.values().associateBy { it.value }

        fun findType(value: Byte): Type = mappings[value] ?: Type.Unknown
    }

    enum class Type(val value: Byte) {
        /**
         * 未知
         */
        Unknown(0x00),

        /**
         * 心跳消息 PING
         *
         * 消息流向：Client <-> Server
         */
        Ping(0x10),

        /**
         * 心跳消息 PONG
         *
         * 消息流向：Client <-> Server
         */
        Pong(0x11),

        /**
         * 建立代理隧道请求
         *
         * 消息流向：Client -> Server
         */
        Request(0x20),

        /**
         * 建立代理隧道响应
         *
         * 消息流向：Client <- Server
         */
        Response(0x21),

        /**
         * 透传消息
         *
         * 消息流向：Client <-> Server
         */
        Transfer(0x30),

        /**
         * 本地连接成功
         *
         * 消息流向：Client -> Server
         */
        LocalConnected(0x40),

        /**
         * 本地连接断开
         *
         * 消息流向：Client -> Server
         */
        LocalDisconnect(0x41),

        /**
         * 远程连接成功
         *
         * 消息流向：Client <- Server
         */
        RemoteConnected(0x42),

        /**
         * 远程断开连接
         *
         * 消息流向：Client <- Server
         */
        RemoteDisconnect(0x43),

        /**
         * 强制下线
         *
         * 消息流向：Client <- Server
         */
        ForceOff(0x50),

        /**
         * 强制下线回复
         *
         * 消息流向：Client -> Server
         */
        ForceOffReply(0x51),
    }
}

/**
 * 未知
 */
object ProtoMsgUnknown : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Unknown
    override val size: Int get() = 1
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }
}

/**
 * 心跳消息 PING
 *
 * 消息流向：Client <-> Server
 */
object ProtoMsgPing : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Ping
    override val size: Int get() = 1
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }
}

/**
 * 心跳消息 PONG
 *
 * 消息流向：Client <-> Server
 */
object ProtoMsgPong : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Pong
    override val size: Int get() = 1
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }
}

/**
 * 建立代理隧道请求
 *
 * 消息流向：Client -> Server
 */
class ProtoMsgRequest(payload: String) : ProtoMsg {
    private val data = payload.toByteArray()
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Request
    override val size: Int get() = 1 + 4 + data.size
    val payload: String by lazy { String(data) }
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgRequest(payload='$payload')"
    }

}

/**
 * 建立代理隧道响应
 *
 * 消息流向：Client <- Server
 */
class ProtoMsgResponse(val status: Boolean, val tunnelId: Long, payload: String) : ProtoMsg {
    private val data = payload.toByteArray()
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Response
    override val size: Int get() = 1 + 1 + 8 + 4 + data.size
    val payload: String by lazy { String(data) }
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeByte(if (status) 1 else 0)
        out.writeLong(tunnelId)
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgResponse(status=$status, tunnelId=$tunnelId, payload='$payload')"
    }

}

/**
 * 透传消息
 *
 * 消息流向：Client <-> Server
 */
class ProtoMsgTransfer(val tunnelId: Long, val sessionId: Long, val data: ByteArray) : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Transfer
    override val size: Int get() = 1 + 8 + 8 + 4 + data.size
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgTransfer(tunnelId=$tunnelId, sessionId=$sessionId, data=${data.contentToHexString()})"
    }
}

/**
 * 本地连接成功
 *
 * 消息流向：Client -> Server
 */
class ProtoMsgLocalConnected(val tunnelId: Long, val sessionId: Long) : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.LocalConnected
    override val size: Int get() = 1 + 8 + 8
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
    }

    override fun toString(): String {
        return "ProtoMsgLocalConnected(tunnelId=$tunnelId, sessionId=$sessionId)"
    }
}

/**
 * 本地连接断开
 *
 * 消息流向：Client -> Server
 */
class ProtoMsgLocalDisconnect(val tunnelId: Long, val sessionId: Long) : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.LocalDisconnect
    override val size: Int get() = 1 + 8 + 8
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
    }

    override fun toString(): String {
        return "ProtoMsgLocalDisconnect(tunnelId=$tunnelId, sessionId=$sessionId)"
    }
}

/**
 * 远程连接成功
 *
 * 消息流向：Client <- Server
 */
class ProtoMsgRemoteConnected(val tunnelId: Long, val sessionId: Long, payload: String) : ProtoMsg {
    private val data = payload.toByteArray()
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.RemoteConnected
    override val size: Int get() = 1 + 8 + 8 + 4 + data.size
    val payload: String by lazy { String(data) }
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgRemoteConnected(tunnelId=$tunnelId, sessionId=$sessionId, payload='$payload')"
    }
}

/**
 * 远程断开连接
 *
 * 消息流向：Client <- Server
 */
class ProtoMsgRemoteDisconnect(val tunnelId: Long, val sessionId: Long, payload: String) : ProtoMsg {
    private val data = payload.toByteArray()
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.RemoteDisconnect
    override val size: Int get() = 1 + 8 + 8 + 4 + data.size
    val payload: String by lazy { String(data) }
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
        out.writeInt(data.size)
        out.writeBytes(data)
    }

    override fun toString(): String {
        return "ProtoMsgRemoteDisconnect(tunnelId=$tunnelId, sessionId=$sessionId, payload='$payload')"
    }
}

/**
 * 强制下线
 *
 * 消息流向：Client <- Server
 */
object ProtoMsgForceOff : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.ForceOff
    override val size: Int get() = 1
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }
}

/**
 * 强制下线回复
 *
 * 消息流向：Client -> Server
 */
object ProtoMsgForceOffReply : ProtoMsg {
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.ForceOffReply
    override val size: Int get() = 1
    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }
}
