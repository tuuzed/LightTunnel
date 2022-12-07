import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.GZIPOutputStream

val File.toHex: String get() = bytesToHexString(readBytes())

val String.toZip: ByteArray get() = zip(toByteArray())

private fun bytesToHexString(bytes: ByteArray?): String {
    if (null == bytes) return ""
    val strBuilder = StringBuilder()
    for (b in bytes) {
        val hex = Integer.toHexString(0xFF and b.toInt())
        if (1 == hex.length) {
            strBuilder.append("0")
        }
        strBuilder.append(hex.toUpperCase(Locale.ENGLISH))
    }
    return strBuilder.toString()
}

@Throws(IOException::class)
private fun zip(data: ByteArray): ByteArray {
    val buffer = ByteArrayOutputStream()
    val zipStream = GZIPOutputStream(buffer)
    zipStream.write(data)
    zipStream.close()
    return buffer.toByteArray()
}
