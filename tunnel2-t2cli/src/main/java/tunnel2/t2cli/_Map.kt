@file:JvmName("_Map")

package tunnel2.t2cli


fun Map<*, *>.getString(key: String, def: String): String {
    val o = this[key] ?: return def
    return if (o is String) o else o.toString()
}

fun Map<*, *>.getMap(key: String): Map<*, *> {
    val o = this[key]
    return if (o is Map<*, *>) o else emptyMap<Any, Any>()
}

fun Map<*, *>.getListMap(key: String): List<Map<*, *>> {
    val o = this[key]
    @Suppress("UNCHECKED_CAST")
    return if (o is List<*>) o as List<Map<*, *>> else emptyList()
}

fun Map<*, *>.getInt(key: String, def: Int): Int {
    val o = this[key] ?: return def
    return try {
        o.toString().toInt()
    } catch (e: Exception) {
        def
    }
}

fun Map<*, *>.getBoolean(key: String, def: Boolean): Boolean {
    val o = this[key] ?: return def
    return try {
        o.toString().toBoolean()
    } catch (e: Exception) {
        def
    }

}