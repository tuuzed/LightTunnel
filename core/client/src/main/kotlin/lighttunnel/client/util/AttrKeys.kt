package lighttunnel.client.util

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import lighttunnel.client.TunnelConnDescriptor
import lighttunnel.proto.ProtoRequest


object AttrKeys {

    val AK_TUNNEL_ID = AttributeKey.newInstance<Long>("AK_TUNNEL_ID")

    val AK_SESSION_ID = AttributeKey.newInstance<Long>("AK_SESSION_ID")

    val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel>("NEXT_CHANNEL")

    val AK_LT_REQUEST = AttributeKey.newInstance<ProtoRequest>("AK_LT_REQUEST")

    val AK_ERR_FLAG = AttributeKey.newInstance<Boolean>("AK_ERR_FLAG")

    val AK_ERR_CAUSE = AttributeKey.newInstance<Throwable>("AK_ERR_CAUSE")

    val AK_LT_CONN_DESCRIPTOR = AttributeKey.newInstance<TunnelConnDescriptor>("AK_LT_CONN_DESCRIPTOR")
}
