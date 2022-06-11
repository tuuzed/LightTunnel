package krp.common.utils

import java.util.*

object HexStringUtils {

    fun bytesToHexString(bytes: ByteArray?): String {
        if (null == bytes) return ""
        val strBuilder = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (1 == hex.length) {
                strBuilder.append("0")
            }
            strBuilder.append(hex.uppercase(Locale.ENGLISH))
        }
        return strBuilder.toString()
    }

    fun hexStringToBytes(hexString: String?): ByteArray {
        if (hexString == null) return ByteArray(0)
        val len = hexString.length / 2
        val result = ByteArray(len)
        val achar = hexString.toCharArray()
        for (i in 0 until len) {
            val pos = i * 2
            result[i] = (charToByte(achar[pos]) shl 4 or charToByte(achar[pos + 1])).toByte()
        }
        return result
    }

    // 字符转字节
    private fun charToByte(c: Char) = "0123456789ABCDEF".indexOf(c)
}
