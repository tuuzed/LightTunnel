package lighttunnel.cmd.util

fun String?.asInt(defVal: Int? = null): Int? {
    return try {
        this?.toInt()
    } catch (e: NumberFormatException) {
        return defVal
    }
}