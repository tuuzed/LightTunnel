@file:JvmName("-ConstsKt")

package lighttunnel.server.util

import io.netty.util.AttributeKey
import lighttunnel.openapi.http.HttpChain

internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("\$lighttunnel.server.SessionId")

internal val AK_SESSION_CHANNELS: AttributeKey<SessionChannels?> = AttributeKey.newInstance("\$lighttunnel.server.SessionChannels")

internal val AK_HTTP_HOST: AttributeKey<String?> = AttributeKey.newInstance("\$lighttunnel.server.HttpHost")

internal val AK_IS_PLUGIN_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("\$lighttunnel.server.isPluginHandle")

internal val AK_IS_INTERCEPTOR_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("\$lighttunnel.server.isInterceptorHandle")

internal val AK_HTTP_CHAIN: AttributeKey<HttpChain?> = AttributeKey.newInstance("\$lighttunnel.server.HttpChain")
