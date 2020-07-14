package lighttunnel.base.util

object LongUtil {

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
        return bytes[offset].toLong() and 0xff shl 56 or (
            bytes[offset + 1].toLong() and 0xff shl 48) or (
            bytes[offset + 2].toLong() and 0xff shl 40) or (
            bytes[offset + 3].toLong() and 0xff shl 32) or (
            bytes[offset + 4].toLong() and 0xff shl 24) or (
            bytes[offset + 5].toLong() and 0xff shl 16) or (
            bytes[offset + 6].toLong() and 0xff shl 8) or (
            bytes[offset + 7].toLong() and 0xff)
    }

}