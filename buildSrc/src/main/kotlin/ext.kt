import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream

val File.hex: String get() = bytesToHexString(readBytes())

val String.zip: ByteArray get() = zip(toByteArray())

val buildDate: String get() = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy ZZZ", Locale.ENGLISH).format(Date())

val commitHash: String get() = runCatching { "git log -1 --pretty=%H".execute().text().trim() }.getOrNull() ?: ""

val commitDate: String get() = runCatching { "git log -1 --pretty=%cd".execute().text().trim() }.getOrNull() ?: ""

private fun String.execute(
    envp: Array<String>? = null, dir: String? = null
) = Runtime.getRuntime().exec(this, envp, if (dir.isNullOrEmpty()) null else File(dir))!!

private fun Process.text() = InputStreamReader(inputStream).readText()

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
