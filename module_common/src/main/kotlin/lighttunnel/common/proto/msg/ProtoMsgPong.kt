package lighttunnel.common.proto.msg

import io.netty.buffer.ByteBuf
import lighttunnel.common.proto.Proto.FLAG_NONE

/**
 * 心跳消息 PONG
 *
 * 消息流向：Client <-> Server
 */
object ProtoMsgPong : ProtoMsg {

    override val flag: Byte = FLAG_NONE
    override val cmd: ProtoMsg.Cmd get() = ProtoMsg.Cmd.Pong
    override val size: Int get() = 1

    override fun transmit(out: ByteBuf) {
        out.writeByte(cmd.value.toInt())
    }

    override fun toString(): String {
        return "ProtoMsgPong()"
    }
}
