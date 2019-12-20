package lighttunnel.proto

import io.netty.buffer.Unpooled

class ProtoMassage(
    val cmd: ProtoCommand,
    val head: ByteArray = ProtoConsts.emptyBytes,
    val data: ByteArray = ProtoConsts.emptyBytes
) {

    val headBuf by lazy { Unpooled.wrappedBuffer(head) }
    val dataBuf by lazy { Unpooled.wrappedBuffer(data) }

}