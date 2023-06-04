package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf

/**
 * 本地连接断开
 *
 * 消息流向：Client -> Server
 */
class ProtoMsgLocalDisconnect(
    val tunnelId: Long,
    val sessionId: Long,
) : ProtoMsg {

    override val flag: Byte = 0
    override val cmd: ProtoMsg.Cmd get() = ProtoMsg.Cmd.LocalDisconnect
    override val size: Int get() = 1 + 8 + 8

    override fun transmit(out: ByteBuf) {
        out.writeByte(cmd.value.toInt())
        out.writeLong(tunnelId)
        out.writeLong(sessionId)
    }

    override fun toString(): String {
        return "ProtoMsgLocalDisconnect(tunnelId=$tunnelId, sessionId=$sessionId)"
    }
}
