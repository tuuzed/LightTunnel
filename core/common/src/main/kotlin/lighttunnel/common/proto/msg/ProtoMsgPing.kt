package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf

/**
 * 心跳消息 PING
 *
 * 消息流向：Client <-> Server
 */
object ProtoMsgPing : ProtoMsg {
    override val flags: Byte = 0
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Ping
    override val size: Int get() = 1

    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }

    override fun toString(): String {
        return "ProtoMsgPing()"
    }
}
