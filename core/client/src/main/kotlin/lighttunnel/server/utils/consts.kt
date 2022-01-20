@file:JvmName("-ConstsKt")

package lighttunnel.server.utils

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.base.entity.TunnelRequest
import lighttunnel.server.conn.impl.TunnelConnImpl
import lighttunnel.server.extra.ChannelInactiveExtra

private const val PREFIX = "\$lighttunnel.client"

internal val AK_TUNNEL_ID = AttributeKey.newInstance<Long?>("$PREFIX.TunnelId")

internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

internal val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel?>("$PREFIX.NextChannel")

internal val AK_TUNNEL_REQUEST = AttributeKey.newInstance<TunnelRequest?>("$PREFIX.TunnelRequest")

internal val AK_TUNNEL_CONN = AttributeKey.newInstance<TunnelConnImpl?>("$PREFIX.TunnelConn")

internal val AK_CHANNEL_INACTIVE_EXTRA = AttributeKey.newInstance<ChannelInactiveExtra?>("$PREFIX.ChannelInactiveExtra")
