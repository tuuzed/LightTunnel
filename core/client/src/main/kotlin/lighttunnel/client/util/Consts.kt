@file:JvmName("-ConstsKt")

package lighttunnel.client.util

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.connect.TunnelConnectFd
import lighttunnel.proto.TunnelRequest

internal val AK_TUNNEL_ID: AttributeKey<Long> = AttributeKey.newInstance("\$lighttunnel.client.tunnel_id")
internal val AK_SESSION_ID: AttributeKey<Long> = AttributeKey.newInstance("\$lighttunnel.client.session_id")
internal val AK_NEXT_CHANNEL: AttributeKey<Channel> = AttributeKey.newInstance("\$lighttunnel.client.next_channel")
internal val AK_TUNNEL_REQUEST: AttributeKey<TunnelRequest> = AttributeKey.newInstance("\$lighttunnel.client.tunnel_request")
internal val AK_FORCED_OFFLINE: AttributeKey<Boolean> = AttributeKey.newInstance("\$lighttunnel.client.forced_offline")
internal val AK_ERROR_CAUSE: AttributeKey<Throwable> = AttributeKey.newInstance("\$lighttunnel.client.error_cause")
internal val AK_TUNNEL_CONNECT_FD: AttributeKey<TunnelConnectFd> = AttributeKey.newInstance("\$lighttunnel.client.tunnel_connect_fd")