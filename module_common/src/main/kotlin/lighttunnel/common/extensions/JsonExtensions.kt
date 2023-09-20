@file:Suppress("unused")

package lighttunnel.common.extensions

import org.json.JSONArray
import org.json.JSONObject

fun <I, O> JSONArray.map(convert: JSONArray.(Int) -> I, transform: I.() -> O): List<O> {
    val list = mutableListOf<O>()
    for (i in 0 until length()) {
        val e = transform(convert(i))
        list.add(e)
    }
    return list
}

inline fun <reified T> JSONObject.opt(key: String, defValue: T): T = optOrNull<T>(key) ?: defValue

inline fun <reified T> JSONArray.optOrNull(index: Int): T? {
    if (index > length() - 1) {
        return null
    }
    return when (T::class.java) {
        Boolean::class.java, java.lang.Boolean::class.java -> runCatching { getBoolean(index) }.getOrNull() as? T
        Byte::class.java, java.lang.Byte::class.java -> runCatching { getInt(index).toByte() }.getOrNull() as? T
        Short::class.java, java.lang.Short::class.java -> runCatching { getInt(index).toShort() }.getOrNull() as? T
        Int::class.java, java.lang.Integer::class.java -> runCatching { getInt(index) }.getOrNull() as? T
        Long::class.java, java.lang.Long::class.java -> runCatching { getLong(index) }.getOrNull() as? T
        Float::class.java, java.lang.Float::class.java -> runCatching { getDouble(index).toFloat() }.getOrNull() as? T
        Double::class.java, java.lang.Double::class.java -> runCatching { getDouble(index) }.getOrNull() as? T
        String::class.java -> runCatching { getString(index) }.getOrNull() as? T
        JSONObject::class.java -> runCatching { getJSONObject(index) }.getOrNull() as? T
        JSONArray::class.java -> runCatching { getJSONArray(index) }.getOrNull() as? T
        else -> null
    }
}

inline fun <reified T> JSONObject.optOrNull(key: String): T? {
    if (!has(key)) return null
    return when (T::class.java) {
        Boolean::class.java, java.lang.Boolean::class.java -> runCatching { getBoolean(key) }.getOrNull() as? T
        Byte::class.java, java.lang.Byte::class.java -> runCatching { getInt(key).toByte() }.getOrNull() as? T
        Short::class.java, java.lang.Short::class.java -> runCatching { getInt(key).toShort() }.getOrNull() as? T
        Int::class.java, java.lang.Integer::class.java -> runCatching { getInt(key) }.getOrNull() as? T
        Long::class.java, java.lang.Long::class.java -> runCatching { getLong(key) }.getOrNull() as? T
        Float::class.java, java.lang.Float::class.java -> runCatching { getDouble(key).toFloat() }.getOrNull() as? T
        Double::class.java, java.lang.Double::class.java -> runCatching { getDouble(key) }.getOrNull() as? T
        String::class.java -> runCatching { getString(key) }.getOrNull() as? T
        JSONObject::class.java -> runCatching { getJSONObject(key) }.getOrNull() as? T
        JSONArray::class.java -> runCatching { getJSONArray(key) }.getOrNull() as? T
        else -> null
    }
}

fun JSONObjectOf(vararg pairs: Pair<String, Any?>): JSONObject {
    val json = ImmutableJSONObject(pairs.size)
    pairs.onEach { json.put(it.first, it.second) }
    return json

}

fun JSONObjectOf(pairs: List<Pair<String, Any?>>): JSONObject {
    val json = ImmutableJSONObject(pairs.size)
    pairs.onEach { json.put(it.first, it.second) }
    return json
}

fun JSONArrayOf(vararg items: Any?): JSONArray {
    val json = JSONArray(items.size)
    items.onEach { json.put(it) }
    return json
}

fun JSONArrayOf(items: List<Any?>): JSONArray {
    val json = JSONArray(items.size)
    items.onEach { json.put(it) }
    return json
}

private class ImmutableJSONObject(initialCapacity: Int) : JSONObject(initialCapacity)

private class ImmutableJSONArray(initialCapacity: Int) : JSONArray(initialCapacity)

