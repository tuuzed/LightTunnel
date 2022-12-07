package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf

/**
 * 强制下线回复
 *
 * 消息流向：Client -> Server
 */
object ProtoMsgForceOffReply : ProtoMsg {

    override val flags: Byte = 0
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.ForceOffReply
    override val size: Int get() = 1

    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }

    override fun toString(): String {
        return "ProtoMsgForceOffReply()"
    }
}
