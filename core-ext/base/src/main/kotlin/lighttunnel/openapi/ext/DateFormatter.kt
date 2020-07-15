package lighttunnel.openapi.ext

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.getOrSet

private val t = ThreadLocal<MutableMap<String, DateFormat>>()

private fun getDateFormat(pattern: String) = t.getOrSet { hashMapOf() }[pattern]
    ?: SimpleDateFormat(pattern, Locale.getDefault()).also { t.get()[pattern] = it }

fun Date?.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String? = this?.let { getDateFormat(pattern).format(this) }
