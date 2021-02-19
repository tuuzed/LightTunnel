@file:JvmName("-ConstsKt")

package lighttunnel.server.utils

import io.netty.util.AttributeKey
import lighttunnel.server.http.impl.HttpContextImpl

private const val PREFIX = "\$lighttunnel.server"

@get:JvmName("AK_SESSION_ID")
internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

@get:JvmName("AK_SESSION_CHANNELS")
internal val AK_SESSION_CHANNELS = AttributeKey.newInstance<SessionChannels?>("$PREFIX.SessionChannels")

@get:JvmName("AK_HTTP_HOST")
internal val AK_HTTP_HOST = AttributeKey.newInstance<String?>("$PREFIX.HttpHost")

@get:JvmName("AK_IS_PLUGIN_HANDLE")
internal val AK_IS_PLUGIN_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isPluginHandle")

@get:JvmName("AK_IS_INTERCEPTOR_HANDLE")
internal val AK_IS_INTERCEPTOR_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isInterceptorHandle")

@get:JvmName("AK_HTTP_CONTEXT")
internal val AK_HTTP_CONTEXT = AttributeKey.newInstance<HttpContextImpl?>("$PREFIX.HttpContext")
