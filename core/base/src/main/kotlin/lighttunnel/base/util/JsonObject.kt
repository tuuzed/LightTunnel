@file:JvmName("-JsonObjectKt")

package lighttunnel.base.util

import org.json.JSONObject

fun JSONObject?.toStringMap(): Map<String, String> {
    this ?: return emptyMap()
    val map = mutableMapOf<String, String>()
    keys().forEach { key ->
        getOrDefault<String?>(key, null)?.also { value ->
            map[key] = value
        }
    }
    return map
}

inline fun <reified T> JSONObject.getOrDefault(
    key: String,
    defValue: T
): T = if (this.has(key)) this.get(key).let { if (it is T) it else defValue } else defValue