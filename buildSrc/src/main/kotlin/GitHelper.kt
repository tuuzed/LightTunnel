import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object GitHelper {

    val buildDate: String get() = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy ZZZ", Locale.ENGLISH).format(Date())

    val commitHash: String get() = runCatching { "git log -1 --pretty=%H".execute().text().trim() }.getOrNull() ?: ""

    val commitDate: String get() = runCatching { "git log -1 --pretty=%cd".execute().text().trim() }.getOrNull() ?: ""

    private fun String.execute(
        envp: Array<String>? = null, dir: String? = null
    ) = Runtime.getRuntime().exec(this, envp, if (dir.isNullOrEmpty()) null else File(dir))!!

    private fun Process.text() = InputStreamReader(inputStream).readText()

}