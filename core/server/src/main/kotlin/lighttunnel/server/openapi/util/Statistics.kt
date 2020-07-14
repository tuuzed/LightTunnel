package lighttunnel.server.openapi.util

import java.util.*

interface Statistics {
    val createAt: Date
    val updateAt: Date
    var inboundBytes: Long
    var outboundBytes: Long
}