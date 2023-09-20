package lighttunnel.common.extensions

fun String?.asInt(defValue: Int? = null): Int? = runCatching { this?.toInt() }.getOrDefault(defValue)
