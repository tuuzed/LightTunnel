@file:JvmName("-ConstsKt")

package lighttunnel.server.utils

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.base.TunnelRequest
import lighttunnel.server.conn.impl.TunnelConnImpl
import lighttunnel.server.extra.ChannelInactiveExtra

private const val PREFIX = "\$lighttunnel.client"

@get:JvmName("AK_TUNNEL_ID")
internal val AK_TUNNEL_ID = AttributeKey.newInstance<Long?>("$PREFIX.TunnelId")

@get:JvmName("AK_SESSION_ID")
internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

@get:JvmName("AK_NEXT_CHANNEL")
internal val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel?>("$PREFIX.NextChannel")

@get:JvmName("AK_TUNNEL_REQUEST")
internal val AK_TUNNEL_REQUEST = AttributeKey.newInstance<TunnelRequest?>("$PREFIX.TunnelRequest")

@get:JvmName("AK_TUNNEL_CONN")
internal val AK_TUNNEL_CONN = AttributeKey.newInstance<TunnelConnImpl?>("$PREFIX.TunnelConn")

@get:JvmName("AK_CHANNEL_INACTIVE_EXTRA")
internal val AK_CHANNEL_INACTIVE_EXTRA = AttributeKey.newInstance<ChannelInactiveExtra?>("$PREFIX.ChannelInactiveExtra")
