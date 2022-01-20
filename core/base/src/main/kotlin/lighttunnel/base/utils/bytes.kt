package lighttunnel.base.utils

/** 空字节数组 */
val emptyBytes = ByteArray(0)

val Short.asBytes: ByteArray get() = ByteArray(2).also { asBytes(0, it) }
fun Short.asBytes(start: Int, dst: ByteArray) {
    for (i in 0 until 2) {
        dst[start + i] = (this.toInt() shr 8 * (2 - 1 - i) and 0xFF).toByte()
    }
}

val Int.asBytes: ByteArray get() = ByteArray(4).also { asBytes(0, it) }
fun Int.asBytes(start: Int, dst: ByteArray) {
    for (i in 0 until 4) {
        dst[start + i] = (this shr 8 * (4 - 1 - i) and 0xFF).toByte()
    }
}

val Long.asBytes: ByteArray get() = ByteArray(8).also { asBytes(0, it) }
fun Long.asBytes(start: Int, dst: ByteArray) {
    for (i in 0 until 8) {
        dst[start + i] = (this shr 8 * (8 - 1 - i) and 0xFF).toByte()
    }
}

fun ByteArray?.contentToHexString(separator: String? = null): String? {
    this ?: return null
    val sb = StringBuffer(this.size * 2)
    var first = true
    for (b in this) {
        if (!first && separator != null) sb.append(separator)
        val hex = Integer.toHexString(b.toInt() and 0xFF)
        if (hex.length == 1) sb.append('0')
        sb.append(hex)
        first = false
    }
    return sb.toString().uppercase()
}
