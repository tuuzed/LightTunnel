package tunnel2.common.proto

enum class ProtoCw(val value: Byte) {
    /**
     * 未知
     */
    UNKNOWN(0x00.toByte()),
    /**
     * 心跳消息 PING
     */
    PING(0x01.toByte()),
    /**
     * 心跳消息 PONG
     */
    PONG(0x02.toByte()),
    /**
     * 建立隧道请求
     */
    REQUEST(0x10.toByte()),
    /**
     * 建立隧道响应失败
     */
    RESPONSE_ERR(0x20.toByte()),
    /**
     * 建立隧道响应成功
     */
    RESPONSE_OK(0x21.toByte()),
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
    ;

    companion object {
        @JvmStatic
        fun ofValue(value: Byte): ProtoCw = values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}