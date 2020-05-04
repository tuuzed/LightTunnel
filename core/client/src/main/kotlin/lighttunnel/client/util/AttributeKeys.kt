package lighttunnel.client.util

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.connect.TunnelConnectFd
import lighttunnel.proto.TunnelRequest


internal object AttributeKeys {

    val AK_TUNNEL_ID: AttributeKey<Long> = AttributeKey.newInstance("\$tunnel_id")

    val AK_SESSION_ID: AttributeKey<Long> = AttributeKey.newInstance("\$session_id")

    val AK_NEXT_CHANNEL: AttributeKey<Channel> = AttributeKey.newInstance("\$next_channel")

    val AK_TUNNEL_REQUEST: AttributeKey<TunnelRequest> = AttributeKey.newInstance("\$tunnel_request")

    val AK_ERROR_CAUSE: AttributeKey<Throwable> = AttributeKey.newInstance("\$error_cause")

    val AK_TUNNEL_CONNECT_FD: AttributeKey<TunnelConnectFd> = AttributeKey.newInstance("\$tunnel_connect_fd")
}
