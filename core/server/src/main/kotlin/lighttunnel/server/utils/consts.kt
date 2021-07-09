@file:JvmName("-ConstsKt")

package lighttunnel.server.utils

import io.netty.util.AttributeKey
import lighttunnel.server.http.impl.HttpContextImpl

private const val PREFIX = "\$lighttunnel.server"

internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

internal val AK_SESSION_CHANNELS = AttributeKey.newInstance<SessionChannels?>("$PREFIX.SessionChannels")

internal val AK_HTTP_HOST = AttributeKey.newInstance<String?>("$PREFIX.HttpHost")

internal val AK_IS_PLUGIN_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isPluginHandle")

internal val AK_IS_INTERCEPTOR_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isInterceptorHandle")

internal val AK_HTTP_CONTEXT = AttributeKey.newInstance<HttpContextImpl?>("$PREFIX.HttpContext")
