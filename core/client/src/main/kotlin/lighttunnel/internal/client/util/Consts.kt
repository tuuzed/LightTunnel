@file:JvmName("-ConstsKt")

package lighttunnel.internal.client.util

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.TunnelRequest
import lighttunnel.internal.client.TunnelClientDaemonChannelHandler
import lighttunnel.internal.client.conn.TunnelConnectionDefaultImpl

@get:JvmName("AK_TUNNEL_ID")
internal val AK_TUNNEL_ID: AttributeKey<Long?> = AttributeKey.newInstance("\$lighttunnel.internal.client.TunnelId")

@get:JvmName("AK_SESSION_ID")
internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("\$lighttunnel.internal.client.SessionId")

@get:JvmName("AK_NEXT_CHANNEL")
internal val AK_NEXT_CHANNEL: AttributeKey<Channel?> = AttributeKey.newInstance("\$lighttunnel.internal.client.NextChannel")

@get:JvmName("AK_TUNNEL_REQUEST")
internal val AK_TUNNEL_REQUEST: AttributeKey<TunnelRequest?> = AttributeKey.newInstance("\$lighttunnel.internal.client.TunnelRequest")

@get:JvmName("AK_TUNNEL_CONNECTION")
internal val AK_TUNNEL_CONNECTION: AttributeKey<TunnelConnectionDefaultImpl?> = AttributeKey.newInstance("\$lighttunnel.internal.client.TunnelConnection")

@get:JvmName("AK_CHANNEL_INACTIVE_EXTRA")
internal val AK_CHANNEL_INACTIVE_EXTRA: AttributeKey<TunnelClientDaemonChannelHandler.ChannelInactiveExtra?> = AttributeKey.newInstance("\$lighttunnel.internal.client.ChannelInactiveExtra")
