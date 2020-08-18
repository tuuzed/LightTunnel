@file:JvmName("-ConstsKt")

package lighttunnel.server.util

import io.netty.util.AttributeKey
import lighttunnel.server.http.HttpContextDefaultImpl

@get:JvmName("AK_SESSION_ID")
internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("\$lighttunnel.server.SessionId")

@get:JvmName("AK_SESSION_CHANNELS")
internal val AK_SESSION_CHANNELS: AttributeKey<SessionChannels?> = AttributeKey.newInstance("\$lighttunnel.server.SessionChannels")

@get:JvmName("AK_HTTP_HOST")
internal val AK_HTTP_HOST: AttributeKey<String?> = AttributeKey.newInstance("\$lighttunnel.server.HttpHost")

@get:JvmName("AK_IS_PLUGIN_HANDLE")
internal val AK_IS_PLUGIN_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("\$lighttunnel.server.isPluginHandle")

@get:JvmName("AK_IS_INTERCEPTOR_HANDLE")
internal val AK_IS_INTERCEPTOR_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("\$lighttunnel.server.isInterceptorHandle")

@get:JvmName("AK_HTTP_CONTEXT")
internal val AK_HTTP_CONTEXT: AttributeKey<HttpContextDefaultImpl?> = AttributeKey.newInstance("\$lighttunnel.server.HttpContext")
