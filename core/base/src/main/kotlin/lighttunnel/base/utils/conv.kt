@file:JvmName("-ConvKt")

package lighttunnel.base.utils

fun LongArray.asBytes(): ByteArray {
    val bytes = ByteArray(this.size * Long.SIZE_BYTES)
    for (i in this.indices) {
        this[i].asBytes(i * Long.SIZE_BYTES, bytes)
    }
    return bytes
}

fun Long.asBytes(): ByteArray {
    val bytes = ByteArray(Long.SIZE_BYTES)
    this.asBytes(0, bytes)
    return bytes
}

private fun Long.asBytes(start: Int, dst: ByteArray) {
    for (i in 0 until Long.SIZE_BYTES) {
        dst[start + i] = (this shl (8 * i) shr (8 * 7)).toByte()
    }
}

@Throws(IndexOutOfBoundsException::class)
fun ByteArray.asLong(offset: Int = 0): Long {
    return (this[offset + 0].toLong() and 0xFF shl (7 * 8)) or
        (this[offset + 1].toLong() and 0xFF shl (6 * 8)) or
        (this[offset + 2].toLong() and 0xFF shl (5 * 8)) or
        (this[offset + 3].toLong() and 0xFF shl (4 * 8)) or
        (this[offset + 4].toLong() and 0xFF shl (3 * 8)) or
        (this[offset + 5].toLong() and 0xFF shl (2 * 8)) or
        (this[offset + 6].toLong() and 0xFF shl (1 * 8)) or
        (this[offset + 7].toLong() and 0xFF shl (0 * 8))
}

fun String?.asInt(defValue: Int? = null): Int? = kotlin.runCatching { this?.toInt() }.getOrDefault(defValue)
