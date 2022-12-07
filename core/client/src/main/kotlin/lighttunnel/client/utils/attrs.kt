package lighttunnel.client.utils

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.conn.DefaultTunnelConn
import lighttunnel.client.extra.ChannelInactiveExtra
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.proto.Proto

private const val PREFIX = "\$lighttunnel.client"

internal val AK_RSA_PRI_KEY = AttributeKey.newInstance<ByteArray?>("$PREFIX.RSAPriKey")

internal val AK_AES128_KEY get() = Proto.AK_AES128_KEY

internal val AK_TUNNEL_ID = AttributeKey.newInstance<Long?>("$PREFIX.TunnelId")

internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

internal val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel?>("$PREFIX.NextChannel")

internal val AK_TUNNEL_REQUEST = AttributeKey.newInstance<TunnelRequest?>("$PREFIX.TunnelRequest")

internal val AK_TUNNEL_CONN = AttributeKey.newInstance<DefaultTunnelConn?>("$PREFIX.TunnelConn")

internal val AK_CHANNEL_INACTIVE_EXTRA = AttributeKey.newInstance<ChannelInactiveExtra?>("$PREFIX.ChannelInactiveExtra")
