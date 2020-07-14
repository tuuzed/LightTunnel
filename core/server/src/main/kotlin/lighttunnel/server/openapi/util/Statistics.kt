package lighttunnel.server.openapi.util

import java.util.*

interface Statistics {
    val createAt: Date
    val updateAt: Date
    val inboundBytes: Long
    val outboundBytes: Long
}