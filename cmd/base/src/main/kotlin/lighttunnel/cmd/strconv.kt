@file:JvmName("-StrconvKt")

package lighttunnel.cmd

fun String?.asInt(defValue: Int? = null): Int? {
    return try {
        this?.toInt()
    } catch (e: NumberFormatException) {
        return defValue
    }
}
