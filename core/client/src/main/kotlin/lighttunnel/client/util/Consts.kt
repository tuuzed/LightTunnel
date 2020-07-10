@file:JvmName("-ConstsKt")

package lighttunnel.client.util

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.conn.TunnelConnection
import lighttunnel.proto.TunnelRequest

internal val AK_TUNNEL_ID: AttributeKey<Long> = AttributeKey.newInstance("\$lighttunnel.client.TunnelId")
internal val AK_SESSION_ID: AttributeKey<Long> = AttributeKey.newInstance("\$lighttunnel.client.SessionId")
internal val AK_NEXT_CHANNEL: AttributeKey<Channel> = AttributeKey.newInstance("\$lighttunnel.client.NextChannel")
internal val AK_TUNNEL_REQUEST: AttributeKey<TunnelRequest> = AttributeKey.newInstance("\$lighttunnel.client.TunnelRequest")
internal val AK_FORCED_OFFLINE: AttributeKey<Boolean> = AttributeKey.newInstance("\$lighttunnel.client.ForcedOffline")
internal val AK_ERROR_CAUSE: AttributeKey<Throwable> = AttributeKey.newInstance("\$lighttunnel.client.ErrorCause")
internal val AK_TUNNEL_CONNECTION: AttributeKey<TunnelConnection> = AttributeKey.newInstance("\$lighttunnel.client.TunnelConnection")