package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf

/**
 * 未知
 */
object ProtoMsgUnknown : ProtoMsg {

    override val flags: Byte = 0
    override val type: ProtoMsg.Type get() = ProtoMsg.Type.Unknown
    override val size: Int get() = 1

    override fun transmit(out: ByteBuf) {
        out.writeByte(type.value.toInt())
    }

    override fun toString(): String {
        return "ProtoMsgUnknown()"
    }
}
