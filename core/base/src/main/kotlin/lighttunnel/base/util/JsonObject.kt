package lighttunnel.base.util

import org.json.JSONObject

fun JSONObject?.toStringMap(): Map<String, String> {
    this ?: return emptyMap()
    val map = mutableMapOf<String, String>()
    this.keys().forEach {
        val value = this.getOrDefault<String?>(it, null)
        if (value != null) {
            map[it] = value
        }
    }
    return map
}

inline fun <reified T> JSONObject.getOrDefault(key: String, def: T): T {
    if (this.has(key)) {
        val value = this.get(key)
        if (value is T) {
            return value
        }
        return def
    } else {
        return def
    }
}