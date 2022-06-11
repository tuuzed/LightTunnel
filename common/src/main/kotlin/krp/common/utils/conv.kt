package krp.common.utils

fun String?.asInt(defValue: Int? = null): Int? = runCatching { this?.toInt() }.getOrDefault(defValue)
