package tpcommon

import io.netty.buffer.Unpooled

data class TPMassage @JvmOverloads constructor(
    val cmd: TPCommand,
    val head: ByteArray = EMPTY_BYTES,
    val data: ByteArray = EMPTY_BYTES
) {

    val headBuf = Unpooled.wrappedBuffer(head)
    val dataBuf = Unpooled.wrappedBuffer(data)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TPMassage

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