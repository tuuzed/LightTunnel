package lighttunnel.traffic

import java.util.*

interface TrafficStats {
    val createAt: Date
    val updateAt: Date
    val inboundBytes: Long
    val outboundBytes: Long
}