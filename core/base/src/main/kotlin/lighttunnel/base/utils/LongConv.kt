package lighttunnel.base.utils

object LongConv {

    fun toBytes(vararg value: Long): ByteArray {
        val bytes = ByteArray(value.size * Long.SIZE_BYTES)
        for (i in value.indices) {
            toBytes(value[i], i * Long.SIZE_BYTES, bytes)
        }
        return bytes
    }

    private fun toBytes(value: Long, start: Int, dst: ByteArray) {
        for (i in 0 until Long.SIZE_BYTES) {
            dst[start + i] = (value shl (8 * i) shr (8 * 7)).toByte()
        }
    }

    fun fromBytes(bytes: ByteArray, offset: Int = 0): Long {
        return (bytes[offset + 0].toLong() and 0xff shl (7 * 8)) or
            (bytes[offset + 1].toLong() and 0xff shl (6 * 8)) or
            (bytes[offset + 2].toLong() and 0xff shl (5 * 8)) or
            (bytes[offset + 3].toLong() and 0xff shl (4 * 8)) or
            (bytes[offset + 4].toLong() and 0xff shl (3 * 8)) or
            (bytes[offset + 5].toLong() and 0xff shl (2 * 8)) or
            (bytes[offset + 6].toLong() and 0xff shl (1 * 8)) or
            (bytes[offset + 7].toLong() and 0xff shl (0 * 8))
    }

}
