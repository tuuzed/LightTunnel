package tpcommon

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator


fun ByteBufAllocator.long2Bytes(vararg value: Long): ByteArray {
    val buf = buffer(value.size * 8)
    value.forEach { buf.writeLong(it) }
    val bytes = ByteArray(buf.readableBytes())
    buf.readBytes(bytes)
    return bytes
}

fun ByteBuf.toBytes(): ByteArray {
    val bytes = ByteArray(readableBytes())
    readBytes(bytes)
    return bytes
}
