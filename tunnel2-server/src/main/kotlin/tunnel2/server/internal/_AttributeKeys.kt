@file:JvmName("AttributeKeys")
@file:Suppress("HasPlatformType")

package tunnel2.server.internal

import io.netty.util.AttributeKey

val AK_SESSION_ID = AttributeKey.newInstance<Long>("AK_SESSION_ID")
val AK_VHOST = AttributeKey.newInstance<String>("AK_VHOST")
val AK_PASSED = AttributeKey.newInstance<Boolean>("AK_PASSED")
val AK_SERVER_SESSION_CHANNELS = AttributeKey.newInstance<ServerSessionChannels>("AK_SERVER_SESSION_CHANNELS")

