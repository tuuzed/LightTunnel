package lighttunnel.common.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.getOrSet

object DateUtils {

    private val cachedSdf = ThreadLocal<MutableMap<String, DateFormat>>()

    fun format(
        date: Date?, pattern: String = "yyyy-MM-dd HH:mm:ss"
    ): String? = date?.let { getDateFormat(pattern).format(it) }

    private fun getDateFormat(pattern: String) = cachedSdf.getOrSet { hashMapOf() }[pattern] ?: SimpleDateFormat(
        pattern, Locale.getDefault()
    ).also { cachedSdf.get()[pattern] = it }

}
