package lighttunnel.server.consts

import io.netty.util.AttributeKey
import lighttunnel.common.proto.Proto
import lighttunnel.server.SessionChannels
import lighttunnel.server.TunnelDescriptor
import lighttunnel.server.http.DefaultHttpContext

private const val PREFIX = "\$lighttunnel.server"

internal val AK_AES128_KEY: AttributeKey<ByteArray?> get() = Proto.AK_AES128_KEY

internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("$PREFIX.SessionId")

internal val AK_SESSION_CHANNELS: AttributeKey<SessionChannels?> = AttributeKey.newInstance("$PREFIX.SessionChannels")

internal val AK_TUNNEL_DESCRIPTOR: AttributeKey<TunnelDescriptor?> = AttributeKey.newInstance("$PREFIX.TunnelDescriptor")

internal val AK_WATCHDOG_TIME_MILLIS: AttributeKey<Long?> = AttributeKey.newInstance("$PREFIX.WatchdogTimeMillis")

internal val AK_HTTP_HOST: AttributeKey<String?> = AttributeKey.newInstance("$PREFIX.HttpHost")

internal val AK_IS_PLUGIN_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("$PREFIX.isPluginHandle")

internal val AK_IS_INTERCEPTOR_HANDLE: AttributeKey<Boolean?> = AttributeKey.newInstance("$PREFIX.isInterceptorHandle")

internal val AK_HTTP_CONTEXT: AttributeKey<DefaultHttpContext?> = AttributeKey.newInstance("$PREFIX.HttpContext")
