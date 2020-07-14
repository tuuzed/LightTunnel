package lighttunnel.base.proto

enum class ProtoMessageType(val code: Byte) {
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
     * data {@see lighttunnel.base.proto.RemoteInfo}
     */
    REMOTE_CONNECTED(0x40.toByte()),

    /**
     * 远程断开连接
     * 消息流向：Client <- Server
     * data {@see lighttunnel.base.proto.RemoteInfo}
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
        fun codeOf(code: Byte) = values().firstOrNull { it.code == code } ?: UNKNOWN
    }

}