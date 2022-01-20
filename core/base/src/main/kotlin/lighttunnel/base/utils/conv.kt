@file:JvmName("-ConvKt")

package lighttunnel.base.utils

fun String?.asInt(defValue: Int? = null): Int? = runCatching { this?.toInt() }.getOrDefault(defValue)
