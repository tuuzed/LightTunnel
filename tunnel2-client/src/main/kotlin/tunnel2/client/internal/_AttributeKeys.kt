@file:JvmName("_AttributeKeys")
@file:Suppress("HasPlatformType")

package tunnel2.client.internal

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import tunnel2.client.TunnelClientDescriptor
import tunnel2.common.TunnelRequest

val AK_TUNNEL_ID = AttributeKey.newInstance<Long>("AK_TUNNEL_TOKEN")

val AK_SESSION_ID = AttributeKey.newInstance<Long>("AK_SESSION_ID")

val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel>("NEXT_CHANNEL")

val AK_TUNNEL_REQUEST = AttributeKey.newInstance<TunnelRequest>("AK_TUNNEL_REQUEST")

val AK_ERR_FLAG = AttributeKey.newInstance<Boolean>("AK_ERR_FLAG")

val AK_ERR_CAUSE = AttributeKey.newInstance<Throwable>("AK_ERR_CAUSE")

val AK_TUNNEL_CLIENT_DESCRIPTOR = AttributeKey.newInstance<TunnelClientDescriptor>("AK_TUNNEL_CLIENT_DESCRIPTOR")
