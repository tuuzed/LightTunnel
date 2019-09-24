package tpclient

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import tpcommon.TPRequest

internal val AK_TUNNEL_ID = AttributeKey.newInstance<Long>("AK_TUNNEL_TOKEN")

internal val AK_SESSION_ID = AttributeKey.newInstance<Long>("AK_SESSION_ID")

internal val AK_NEXT_CHANNEL = AttributeKey.newInstance<Channel>("NEXT_CHANNEL")

internal val AK_TP_REQUEST = AttributeKey.newInstance<TPRequest>("AK_TP_REQUEST")

internal val AK_ERR_FLAG = AttributeKey.newInstance<Boolean>("AK_ERR_FLAG")

internal val AK_ERR_CAUSE = AttributeKey.newInstance<Throwable>("AK_ERR_CAUSE")

internal val AK_TPC_DESCRIPTOR = AttributeKey.newInstance<TPClientDescriptor>("AK_TPC_DESCRIPTOR")