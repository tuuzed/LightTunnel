package lighttunnel.common.utils

import java.util.*

object FileUtils {

    fun toFileSize(value: String?, defVal: Long): Long {
        if (value == null) return defVal
        var s = value.trim { it <= ' ' }.uppercase(Locale.getDefault())
        var multiplier: Long = 1
        var index: Int
        if (s.indexOf("KB").also { index = it } != -1) {
            multiplier = 1024
            s = s.substring(0, index)
        } else if (s.indexOf("MB").also { index = it } != -1) {
            multiplier = (1024 * 1024).toLong()
            s = s.substring(0, index)
        } else if (s.indexOf("GB").also { index = it } != -1) {
            multiplier = (1024 * 1024 * 1024).toLong()
            s = s.substring(0, index)
        }
        try {
            return java.lang.Long.valueOf(s).toLong() * multiplier
        } catch (e: NumberFormatException) {
            System.err.println("[$s] is not in proper int form.")
            System.err.println("[$value] not in expected format.")
            e.printStackTrace()
        }
        return defVal
    }
}
