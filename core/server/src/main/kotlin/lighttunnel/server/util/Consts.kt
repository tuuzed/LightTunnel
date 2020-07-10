@file:JvmName("-ConstsKt")

package lighttunnel.server.util

import io.netty.util.AttributeKey
import org.json.JSONArray

internal val EMPTY_JSON_ARRAY = JSONArray(emptyList<Any>())

internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("\$lighttunnel.server.SessionId")

internal val AK_SESSION_CHANNELS: AttributeKey<SessionChannels?> = AttributeKey.newInstance("\$lighttunnel.server.SessionChannels")

internal val AK_HTTP_HOST: AttributeKey<String?> = AttributeKey.newInstance("\$lighttunnel.server.HttpHost")
