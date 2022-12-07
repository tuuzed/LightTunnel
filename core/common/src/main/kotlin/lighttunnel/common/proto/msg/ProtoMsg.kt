package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf

sealed interface ProtoMsg {
    val flags: Byte
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
         * 握手，交互加密秘钥
         *
         * 消息流向：Client <-> Server
         */
        Handshake(0x12),

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
