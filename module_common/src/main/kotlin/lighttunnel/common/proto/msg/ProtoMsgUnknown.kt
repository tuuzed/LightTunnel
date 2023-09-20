package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf
import lighttunnel.common.proto.Proto.FLAG_NONE

/**
 * 未知
 */
object ProtoMsgUnknown : ProtoMsg {

    override val flag: Byte = FLAG_NONE
    override val cmd: ProtoMsg.Cmd get() = ProtoMsg.Cmd.Unknown
    override val size: Int get() = 1

    override fun transmit(out: ByteBuf) {
        out.writeByte(cmd.value.toInt())
    }

    override fun toString(): String {
        return "ProtoMsgUnknown()"
    }
}
