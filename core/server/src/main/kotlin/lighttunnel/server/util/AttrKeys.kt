package lighttunnel.server.util

import io.netty.util.AttributeKey
import lighttunnel.server.SessionPool

object AttrKeys {

    val AK_SESSION_ID = AttributeKey.newInstance<Long>("AK_SESSION_ID")

    val AK_SESSION_POOL = AttributeKey.newInstance<SessionPool>("AK_SESSION_POOL")

    val AK_HTTP_HOST = AttributeKey.newInstance<String>("AK_HTTP_HOST")

    val AK_HTTP_SKIP = AttributeKey.newInstance<Boolean>("AK_HTTP_SKIP")

}