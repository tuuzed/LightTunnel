@file:JvmName("-JsonObjectKt")

package lighttunnel.openapi

import org.json.JSONObject

inline fun <reified T> JSONObject.getOrNull(key: String): T? = if (this.has(key)) this.get(key).let { if (it is T) it else null } else null
inline fun <reified T> JSONObject.getOrDefault(key: String, defValue: T): T = if (this.has(key)) this.get(key).let { if (it is T) it else defValue } else defValue