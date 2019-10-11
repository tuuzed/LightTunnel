package com.tuuzed.lighttunnel.common

import io.netty.buffer.Unpooled

data class LTMassage @JvmOverloads constructor(
    val cmd: LTCommand,
    val head: ByteArray = EMPTY_BYTES,
    val data: ByteArray = EMPTY_BYTES
) {

    val headBuf by lazy { Unpooled.wrappedBuffer(head) }
    val dataBuf by lazy { Unpooled.wrappedBuffer(data) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LTMassage

        if (cmd != other.cmd) return false
        if (!head.contentEquals(other.head)) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cmd.hashCode()
        result = 31 * result + head.contentHashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

}