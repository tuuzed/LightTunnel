package lighttunnel.server.util

import io.netty.util.AttributeKey
import lighttunnel.server.SessionChannels

object AttributeKeys {

    val AK_SESSION_ID: AttributeKey<Long> = AttributeKey.newInstance("\$session_id")
    val AK_SESSION_CHANNELS: AttributeKey<SessionChannels> = AttributeKey.newInstance("\$session_channels")
    val AK_HTTP_HOST: AttributeKey<String> = AttributeKey.newInstance("\$http_host")
    val AK_HTTP_SKIP: AttributeKey<Boolean> = AttributeKey.newInstance("\$http_skip")

}