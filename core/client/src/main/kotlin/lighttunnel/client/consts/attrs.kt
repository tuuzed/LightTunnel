package lighttunnel.client.consts

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.conn.DefaultTunnelConn
import lighttunnel.client.extra.ChannelInactiveExtra
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.proto.Proto

private const val PREFIX = "\$lighttunnel.client"

internal val AK_RSA_PRI_KEY: AttributeKey<ByteArray?> = AttributeKey.newInstance("$PREFIX.RSAPriKey")

internal val AK_AES128_KEY: AttributeKey<ByteArray?> get() = Proto.AK_AES128_KEY

internal val AK_TUNNEL_ID: AttributeKey<Long?> = AttributeKey.newInstance("$PREFIX.TunnelId")

internal val AK_SESSION_ID: AttributeKey<Long?> = AttributeKey.newInstance("$PREFIX.SessionId")

internal val AK_NEXT_CHANNEL: AttributeKey<Channel?> = AttributeKey.newInstance("$PREFIX.NextChannel")

internal val AK_TUNNEL_REQUEST: AttributeKey<TunnelRequest?> = AttributeKey.newInstance("$PREFIX.TunnelRequest")

internal val AK_TUNNEL_CONN: AttributeKey<DefaultTunnelConn?> = AttributeKey.newInstance("$PREFIX.TunnelConn")

internal val AK_CHANNEL_INACTIVE_EXTRA: AttributeKey<ChannelInactiveExtra?> = AttributeKey.newInstance("$PREFIX.ChannelInactiveExtra")
